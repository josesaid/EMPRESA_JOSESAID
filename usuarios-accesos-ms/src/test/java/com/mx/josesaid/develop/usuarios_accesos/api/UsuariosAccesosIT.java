package com.mx.josesaid.develop.usuarios_accesos.api;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.usuarios_accesos.application.UsuarioService;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
@SpringBootTest(properties = "usuarios.jwt.secreto=clave-de-prueba-de-al-menos-32-bytes-xx")
@AutoConfigureMockMvc
@Testcontainers
class UsuariosAccesosIT {

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

    @Test
    void loginEmiteTokenYLaRutaProtegidaRechazaSinToken() throws Exception {
        servicio.crear("admin@empresa.mx", "Administrador General", "clave-admin-123", Set.of(Rol.ADMIN));

        mvc.perform(post("/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"correo\":\"admin@empresa.mx\",\"password\":\"clave-admin-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(notNullValue()))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));

        mvc.perform(get("/v1/usuarios/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());
    }
}
