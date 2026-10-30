package com.mx.josesaid.develop.contabilidad.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integración con PostgreSQL y Kafka reales (Testcontainers). Requiere Docker; corre con 'mvn verify'. */
@SpringBootTest(properties = "contabilidad.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@AutoConfigureMockMvc
@Testcontainers
class ContabilidadIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    @Test
    void flujoContableConRolesYCierre() throws Exception {
        mvc.perform(get("/v1/estados-financieros?periodo=2026-10"))
                .andExpect(status().isUnauthorized());

        // DIRECCION no escribe
        mvc.perform(post("/v1/cuentas").with(rol("DIRECCION"))
                        .contentType("application/json")
                        .content("{\"codigo\":\"1101\",\"nombre\":\"Caja\",\"naturaleza\":\"DEUDORA\"}"))
                .andExpect(status().isForbidden());

        String caja = crearCuenta("1101", "Caja", "DEUDORA");
        String ventas = crearCuenta("4101", "Ventas", "ACREEDORA");

        // Póliza descuadrada: 422
        mvc.perform(post("/v1/polizas").with(rol("CONTADOR"))
                        .contentType("application/json")
                        .content(poliza(caja, ventas, "100", "90")))
                .andExpect(status().isUnprocessableEntity());

        // Póliza cuadrada: 201
        MvcResult ok = mvc.perform(post("/v1/polizas").with(rol("CONTADOR"))
                        .contentType("application/json")
                        .content(poliza(caja, ventas, "100", "100")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.periodo").value("2026-10"))
                .andReturn();
        String polizaId = JsonPath.read(ok.getResponse().getContentAsString(), "$.id");

        // Cierre y bloqueo de movimientos
        mvc.perform(post("/v1/periodos/2026-10/cierre").with(rol("CONTADOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("CERRADO"));
        mvc.perform(post("/v1/polizas").with(rol("CONTADOR"))
                        .contentType("application/json")
                        .content(poliza(caja, ventas, "10", "10")))
                .andExpect(status().isConflict());

        // Consulta de estados financieros por DIRECCION
        mvc.perform(get("/v1/estados-financieros?periodo=2026-10").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuadra").value(true));

        mvc.perform(get("/v1/polizas/" + polizaId).with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("APLICADA"));
    }

    private String crearCuenta(String codigo, String nombre, String naturaleza) throws Exception {
        MvcResult r = mvc.perform(post("/v1/cuentas").with(rol("CONTADOR"))
                        .contentType("application/json")
                        .content("{\"codigo\":\"" + codigo + "\",\"nombre\":\"" + nombre + "\",\"naturaleza\":\"" + naturaleza + "\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private static String poliza(String cuentaCargo, String cuentaAbono, String cargo, String abono) {
        return "{\"tipo\":\"INGRESO\",\"fecha\":\"2026-10-15\",\"movimientos\":["
                + "{\"cuentaId\":\"" + cuentaCargo + "\",\"cargo\":" + cargo + "},"
                + "{\"cuentaId\":\"" + cuentaAbono + "\",\"abono\":" + abono + "}]}";
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void mismaIdempotencyKeyNoDuplicaLaPoliza() throws Exception {
        int sufijo = new java.util.Random().nextInt(900) + 100;
        String caja = crearCuenta("1201-" + sufijo, "Caja replay", "DEUDORA");
        String ventas = crearCuenta("4201-" + sufijo, "Ventas replay", "ACREEDORA");
        String cuerpo = "{\"tipo\":\"DIARIO\",\"fecha\":\"2026-09-15\",\"movimientos\":["
                + "{\"cuentaId\":\"" + caja + "\",\"cargo\":50},"
                + "{\"cuentaId\":\"" + ventas + "\",\"abono\":50}]}";
        String clave = "prueba-" + java.util.UUID.randomUUID();
        MvcResult primero = mvc.perform(post("/v1/polizas").with(rol("CONTADOR")).header("Idempotency-Key", clave)
                        .contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        MvcResult reintento = mvc.perform(post("/v1/polizas").with(rol("CONTADOR")).header("Idempotency-Key", clave)
                        .contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        assertEquals((String) JsonPath.read(primero.getResponse().getContentAsString(), "$.id"),
                (String) JsonPath.read(reintento.getResponse().getContentAsString(), "$.id"));
    }
}
