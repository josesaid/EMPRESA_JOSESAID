package com.mx.josesaid.develop.usuarios_accesos.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.usuarios_accesos.application.UsuarioService;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import java.security.KeyPairGenerator;
import java.util.Base64;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Prueba de integración con PostgreSQL y Kafka reales (Testcontainers).
 * Requiere Docker. Se ejecuta con 'mvn verify'.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class UsuariosAccesosIT {

    /** Correo único por ejecución: no choca con administradores de otras corridas o del entorno. */
    private static final String CORREO_ADMIN = "admin-" + java.util.UUID.randomUUID() + "@prueba.mx";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private UsuarioService servicio;

    /** Genera una clave RSA de prueba en cada ejecución; no se guarda en el repositorio. */
    @DynamicPropertySource
    static void claveJwtDePrueba(DynamicPropertyRegistry registro) throws Exception {
        KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        String pkcs8 = Base64.getEncoder().encodeToString(generador.generateKeyPair().getPrivate().getEncoded());
        registro.add("usuarios.jwt.clave-privada", () -> pkcs8);
    }

    @Test
    void loginEmiteTokenYLaRutaProtegidaRechazaSinToken() throws Exception {
        servicio.crear(CORREO_ADMIN, "Administrador General", "clave-admin-123", Set.of(Rol.ADMIN));

        mvc.perform(post("/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"correo\":\"" + CORREO_ADMIN + "\",\"password\":\"clave-admin-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mvc.perform(get("/v1/usuarios/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refrescoRotaYElReusoRevocaLaSesion() throws Exception {
        String correo = "sesion-" + java.util.UUID.randomUUID() + "@prueba.mx";
        servicio.crear(correo, "Usuario de Sesión", "clave-sesion-123", Set.of(Rol.VENTAS));
        String r1 = campo(iniciarSesion(correo), "refreshToken");

        String refrescado = mvc.perform(post("/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"" + r1 + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String r2 = campo(refrescado, "refreshToken");
        assertNotEquals(r1, r2);

        // Reusar el token ya rotado revoca la sesión completa, incluido el token nuevo
        mvc.perform(post("/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"" + r1 + "\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"" + r2 + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logoutCierraLaSesionYLogoutTodasCierraTodas() throws Exception {
        String correo = "logout-" + java.util.UUID.randomUUID() + "@prueba.mx";
        servicio.crear(correo, "Usuario de Logout", "clave-logout-123", Set.of(Rol.VENTAS));
        String sesion1 = iniciarSesion(correo);
        String r1 = campo(sesion1, "refreshToken");

        mvc.perform(post("/v1/auth/logout").contentType("application/json")
                        .content("{\"refreshToken\":\"" + r1 + "\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"" + r1 + "\"}"))
                .andExpect(status().isUnauthorized());

        String sesion2 = iniciarSesion(correo);
        String r2 = campo(sesion2, "refreshToken");
        String acceso = campo(sesion2, "accessToken");
        mvc.perform(post("/v1/auth/logout-todas").header("Authorization", "Bearer " + acceso))
                .andExpect(status().isNoContent());
        mvc.perform(post("/v1/auth/refresh").contentType("application/json")
                        .content("{\"refreshToken\":\"" + r2 + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void passwordTemporalBloqueaElLoginHastaCambiarla() throws Exception {
        String correo = "temporal-" + java.util.UUID.randomUUID() + "@prueba.mx";
        String temporal = "Temporal-inicial-123";
        String nueva = "Nueva-clave-segura-456";
        servicio.crearConPasswordTemporal(correo, "Usuario Temporal", temporal, Set.of(Rol.VENTAS));

        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"" + temporal + "\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(post("/v1/auth/cambiar-password-inicial").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"passwordActual\":\"" + temporal
                                + "\",\"passwordNueva\":\"" + nueva + "\"}"))
                .andExpect(status().isNoContent());

        mvc.perform(post("/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"" + nueva + "\"}"))
                .andExpect(status().isOk());
    }

    private String iniciarSesion(String correo) throws Exception {
        return mvc.perform(post("/v1/auth/login").contentType("application/json")
                        .content("{\"correo\":\"" + correo + "\",\"password\":\"" + clave(correo) + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private static String clave(String correo) {
        return correo.startsWith("logout-") ? "clave-logout-123" : "clave-sesion-123";
    }

    private static String campo(String json, String nombre) {
        java.util.regex.Matcher m = java.util.regex.Pattern
                .compile("\"" + nombre + "\":\"([^\"]+)\"").matcher(json);
        if (!m.find()) {
            throw new IllegalStateException("No se encontró " + nombre);
        }
        return m.group(1);
    }
}
