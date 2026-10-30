package com.mx.josesaid.develop.facturacion_pagos.api;

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
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Integración con PostgreSQL y Kafka reales (Testcontainers). Requiere Docker; se ejecuta con 'mvn verify'.
 * La autenticación se simula con el post-procesador jwt() de spring-security-test.
 */
@SpringBootTest(properties = "facturacion.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@AutoConfigureMockMvc
@Testcontainers
class FacturacionPagosIT {

    private static final String ORDEN = "00000000-0000-0000-0000-000000000001";

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    @Test
    void flujoDeComprobanteConRolesYReglas() throws Exception {
        // Sin token: no autenticado
        mvc.perform(get("/v1/comprobantes/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());

        // VENTAS no puede crear comprobantes
        mvc.perform(post("/v1/comprobantes").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENTAS")))
                        .contentType("application/json")
                        .content("{\"ordenId\":\"" + ORDEN + "\",\"moneda\":\"MXN\",\"subtotal\":1000,\"tasaIva\":0.16}"))
                .andExpect(status().isForbidden());

        // FINANZAS crea, emite y cobra
        MvcResult creado = mvc.perform(post("/v1/comprobantes").with(finanzas())
                        .contentType("application/json")
                        .content("{\"ordenId\":\"" + ORDEN + "\",\"moneda\":\"MXN\",\"subtotal\":1000,\"tasaIva\":0.16}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(1160.00))
                .andReturn();
        String id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/v1/comprobantes/" + id + "/emision").with(finanzas()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("EMITIDO"))
                .andExpect(jsonPath("$.folio").isNotEmpty());

        mvc.perform(post("/v1/comprobantes/" + id + "/pagos").with(finanzas())
                        .contentType("application/json")
                        .content("{\"monto\":2000}"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(post("/v1/comprobantes/" + id + "/pagos").with(finanzas())
                        .contentType("application/json")
                        .content("{\"monto\":1160,\"referencia\":\"SPEI-1\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estatusCobro").value("PAGADO"))
                .andExpect(jsonPath("$.pagos.length()").value(1));

        mvc.perform(get("/v1/comprobantes/" + id).with(jwt().authorities(new SimpleGrantedAuthority("ROLE_VENTAS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(0));
    }

    private static org.springframework.test.web.servlet.request.RequestPostProcessor finanzas() {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_FINANZAS"));
    }

    @Test
    void mismaIdempotencyKeyNoDuplicaElPago() throws Exception {
        MvcResult creado = mvc.perform(post("/v1/comprobantes").with(finanzas())
                        .contentType("application/json")
                        .content("{\"ordenId\":\"00000000-0000-0000-0000-000000000003\",\"moneda\":\"MXN\",\"subtotal\":500,\"tasaIva\":0.16}"))
                .andExpect(status().isCreated()).andReturn();
        String id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/v1/comprobantes/" + id + "/emision").with(finanzas())).andExpect(status().isOk());

        String cuerpo = "{\"monto\":500,\"referencia\":\"SPEI-IDEM\"}";
        String clave = "prueba-" + java.util.UUID.randomUUID();
        MvcResult primero = mvc.perform(post("/v1/comprobantes/" + id + "/pagos").with(finanzas())
                        .header("Idempotency-Key", clave).contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        MvcResult reintento = mvc.perform(post("/v1/comprobantes/" + id + "/pagos").with(finanzas())
                        .header("Idempotency-Key", clave).contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        assertEquals(primero.getResponse().getContentAsString(), reintento.getResponse().getContentAsString());
    }
}
