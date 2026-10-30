package com.mx.josesaid.develop.reportes.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integración con PostgreSQL y Kafka reales (Testcontainers). Requiere Docker; corre con 'mvn verify'. */
@SpringBootTest(properties = "reportes.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@AutoConfigureMockMvc
@Testcontainers
class ReportesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    @Test
    void indicadoresCierreYExportacionFuncionanExtremoAExtremo() throws Exception {
        registrar("Órdenes cerradas", "OPERACIONES", Instant.now().toString(), "12.5").andExpect(status().isCreated());
        registrar("Cartera vencida", "FINANZAS_CONTABILIDAD", "2020-01-01T00:00:00Z", "3").andExpect(status().isCreated());

        mvc.perform(get("/v1/indicadores?periodo=2026-10").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.nombre=='Cartera vencida')].desactualizado").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.nombre=='Órdenes cerradas')].desactualizado").value(hasItem(false)));

        MvcResult reporte = mvc.perform(get("/v1/reportes/2026-10").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("BORRADOR"))
                .andReturn();
        String id = JsonPath.read(reporte.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/v1/reportes/2026-10/cierre").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("CERRADO"))
                .andExpect(jsonPath("$.indicadores.length()").value(2));

        // Cerrado: no se cierra otra vez ni admite indicadores nuevos
        mvc.perform(post("/v1/reportes/2026-10/cierre").with(rol("DIRECCION")))
                .andExpect(status().isConflict());
        registrar("Ventas del mes", "VENTAS_ATENCION_CLIENTES", Instant.now().toString(), "99")
                .andExpect(status().isConflict());

        mvc.perform(get("/v1/reportes/" + id + "/exportacion?formato=CSV").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cartera vencida")));
        mvc.perform(get("/v1/reportes/" + id + "/exportacion?formato=PDF").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/v1/reportes/" + id + "/exportacion?formato=XLSX").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        mvc.perform(get("/v1/reportes/" + id + "/exportacion?formato=DOCX").with(rol("DIRECCION")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rolesPeriodosInvalidosYReportesInexistentes() throws Exception {
        mvc.perform(get("/v1/indicadores?periodo=2026-10").with(rol("ALMACEN")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/indicadores?periodo=2026-13").with(rol("DIRECCION")))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/v1/reportes/2024-01").with(rol("DIRECCION")))
                .andExpect(status().isNotFound());
    }

    @Test
    void jefeDeAreaSoloConsultaSuArea() throws Exception {
        registrarEn("2026-09", "Ventas", "VENTAS_ATENCION_CLIENTES", Instant.now().toString(), "5")
                .andExpect(status().isCreated());
        registrarEn("2026-09", "Cartera", "FINANZAS_CONTABILIDAD", Instant.now().toString(), "7")
                .andExpect(status().isCreated());

        mvc.perform(get("/v1/indicadores?periodo=2026-09").with(jefe("VENTAS_ATENCION_CLIENTES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Ventas"));

        mvc.perform(get("/v1/indicadores?periodo=2026-09&area=FINANZAS_CONTABILIDAD")
                        .with(jefe("VENTAS_ATENCION_CLIENTES")))
                .andExpect(status().isForbidden());

        mvc.perform(get("/v1/indicadores?periodo=2026-09&area=VENTAS_ATENCION_CLIENTES")
                        .with(jefe("VENTAS_ATENCION_CLIENTES")))
                .andExpect(status().isOk());

        mvc.perform(get("/v1/reportes/2026-09").with(jefe("VENTAS_ATENCION_CLIENTES")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.indicadores.length()").value(1));

        // Un jefe no carga ni cierra
        registrarEn("2026-09", "Otra", "VENTAS_ATENCION_CLIENTES", Instant.now().toString(), "1")
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/reportes/2026-09/cierre").with(jefe("VENTAS_ATENCION_CLIENTES")))
                .andExpect(status().isForbidden());
    }

    private ResultActions registrarEn(String periodo, String nombre, String area, String fuente, String valor)
            throws Exception {
        String cuerpo = """
                {"area":"%s","nombre":"%s","periodo":"%s","valor":%s,"fuenteActualizadaUtc":"%s"}
                """.formatted(area, nombre, periodo, valor, fuente);
        RequestPostProcessor quien = nombre.equals("Otra") ? jefe(area) : rol("DIRECCION");
        return mvc.perform(post("/v1/indicadores").with(quien)
                .contentType("application/json").content(cuerpo));
    }

    private static RequestPostProcessor jefe(String area) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_JEFE_AREA"))
                .jwt(j -> j.claim("area", area));
    }

    private ResultActions registrar(String nombre, String area, String fuente, String valor) throws Exception {
        String cuerpo = """
                {"area":"%s","nombre":"%s","periodo":"2026-10","valor":%s,"fuenteActualizadaUtc":"%s"}
                """.formatted(area, nombre, valor, fuente);
        return mvc.perform(post("/v1/indicadores").with(rol("DIRECCION"))
                .contentType("application/json").content(cuerpo));
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
