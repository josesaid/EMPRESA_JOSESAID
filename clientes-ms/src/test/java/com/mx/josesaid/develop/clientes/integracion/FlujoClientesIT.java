package com.mx.josesaid.develop.clientes.integracion;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Pruebas de integración con PostgreSQL 17 y Kafka reales (Testcontainers). Requieren Docker en ejecución.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class FlujoClientesIT {

    private static final String VENDEDOR = "VENDEDOR";
    private static final String ADMIN = "ADMIN_CLIENTES";
    private static final String GERENTE = "GERENTE_VENTAS";
    private static final Pattern ID = Pattern.compile("\"id\":\"([0-9a-f-]{36})\"");

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"));

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    /** Las pruebas usan jwt() de spring-security-test; este decodificador nunca se invoca. */
    @TestConfiguration
    static class ConfiguracionPruebas {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                throw new JwtException("No se usa en pruebas de integración");
            };
        }
    }

    private static RequestPostProcessor como(String rol) {
        return jwt().jwt(j -> j.subject("8f3c2a1e-6b4d-4e5f-9a7b-1c2d3e4f5a6b"))
                .authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    private static String extraerId(MvcResult resultado) throws Exception {
        Matcher m = ID.matcher(resultado.getResponse().getContentAsString());
        m.find();
        return m.group(1);
    }

    @Test
    void conversionDeProspectoCalificadoCreaClienteActivo() throws Exception {
        MvcResult prospecto = mvc.perform(post("/v1/prospectos").with(como(VENDEDOR))
                        .contentType("application/json")
                        .content("""
                                {"nombre":"Ana Flujo","correo":"ana.flujo@ejemplo.mx","fuente":"CAMPANA"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String prospectoId = extraerId(prospecto);

        mvc.perform(patch("/v1/prospectos/" + prospectoId + "/estatus").with(como(VENDEDOR))
                        .contentType("application/json").content("{\"estatus\":\"CONTACTADO\"}"))
                .andExpect(status().isOk());
        mvc.perform(patch("/v1/prospectos/" + prospectoId + "/estatus").with(como(VENDEDOR))
                        .contentType("application/json").content("{\"estatus\":\"CALIFICADO\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/v1/prospectos/" + prospectoId + "/conversion").with(como(VENDEDOR))
                        .contentType("application/json").content("""
                                {"razonSocial":"Industrias Flujo SA de CV","rfc":"IFL900101XY1",
                                 "contactoPrincipal":{"nombre":"Luis Pérez","correo":"luis.flujo@demo.mx","rol":"COMPRAS"}}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estatus").value("ACTIVO"))
                .andExpect(jsonPath("$.rfc").value(containsString("*")));
    }

    @Test
    void segundoClienteActivoConMismoRfcRespondeConflicto() throws Exception {
        String cuerpo = """
                {"razonSocial":"Comercial Rfc SA","rfc":"CRF900101AB2",
                 "contactoPrincipal":{"nombre":"Marta Ruiz","correo":"marta.rfc@norte.mx","rol":"PAGOS"}}
                """;
        mvc.perform(post("/v1/clientes").with(como(ADMIN)).contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated());

        mvc.perform(post("/v1/clientes").with(como(ADMIN)).contentType("application/json").content(cuerpo))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:clientes:error:rfc-duplicado"))
                .andExpect(jsonPath("$.clienteExistenteId").exists());
    }

    @Test
    void clienteDadoDeBajaNoSeReactiva() throws Exception {
        MvcResult alta = mvc.perform(post("/v1/clientes").with(como(ADMIN)).contentType("application/json").content("""
                        {"razonSocial":"Baja Test SA","rfc":"BAJ900101AB3",
                         "contactoPrincipal":{"nombre":"Jorge Baja","correo":"jorge.baja@test.mx","rol":"ASISTENCIA"}}
                        """))
                .andExpect(status().isCreated())
                .andReturn();
        String clienteId = extraerId(alta);

        mvc.perform(patch("/v1/clientes/" + clienteId + "/estatus").with(como(ADMIN))
                        .contentType("application/json").content("{\"estatus\":\"BAJA\"}"))
                .andExpect(status().isOk());

        mvc.perform(patch("/v1/clientes/" + clienteId + "/estatus").with(como(ADMIN))
                        .contentType("application/json").content("{\"estatus\":\"ACTIVO\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("urn:clientes:error:transicion-invalida"));
    }

    @Test
    void vendedorNoPuedeConsultarMetricas() throws Exception {
        mvc.perform(get("/v1/metricas/conversion").param("mes", "2026-10").with(como(VENDEDOR)))
                .andExpect(status().isForbidden());
    }

    @Test
    void gerenteConsultaMetricasDelMes() throws Exception {
        mvc.perform(get("/v1/metricas/conversion").param("mes", "2026-10").with(como(GERENTE)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-10"));
    }
}
