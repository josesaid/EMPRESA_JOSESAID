package com.mx.josesaid.develop.compras.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
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
@SpringBootTest(properties = {
        "compras.jwt.secreto=clave-de-prueba-de-al-menos-32-bytes-xx",
        "compras.cifrado.clave=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "compras.cifrado.indice-clave=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@AutoConfigureMockMvc
@Testcontainers
class ComprasIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    @Test
    void flujoDeCompraConRolesRecepcionesYRfcEnmascarado() throws Exception {
        MvcResult prov = mvc.perform(post("/v1/proveedores").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content("{\"razonSocial\":\"Aceros del Norte SA\",\"rfc\":\"AAN010101AB3\",\"correo\":\"ventas@aceros.mx\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rfc").value("AAN*******B3"))
                .andReturn();
        String proveedor = JsonPath.read(prov.getResponse().getContentAsString(), "$.id");

        // RFC duplicado: 409
        mvc.perform(post("/v1/proveedores").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content("{\"razonSocial\":\"Otro\",\"rfc\":\"aan010101ab3\",\"correo\":\"x@y.mx\"}"))
                .andExpect(status().isConflict());

        MvcResult orden = mvc.perform(post("/v1/ordenes-compra").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content(ordenJson(proveedor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estatus").value("BORRADOR"))
                .andExpect(jsonPath("$.total").value(1000))
                .andReturn();
        String ordenId = JsonPath.read(orden.getResponse().getContentAsString(), "$.id");

        // Solo DIRECCION aprueba
        mvc.perform(post("/v1/ordenes-compra/" + ordenId + "/aprobacion").with(rol("COMPRAS")))
                .andExpect(status().isForbidden());
        mvc.perform(post("/v1/ordenes-compra/" + ordenId + "/aprobacion").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("APROBADA"));

        // Recepción mayor a lo ordenado: 409
        mvc.perform(post("/v1/ordenes-compra/" + ordenId + "/recepciones").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content("{\"lineas\":[{\"articuloSku\":\"TORN-1\",\"cantidad\":11}]}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/v1/ordenes-compra/" + ordenId + "/recepciones").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content("{\"lineas\":[{\"articuloSku\":\"TORN-1\",\"cantidad\":4}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estatus").value("RECIBIDA_PARCIAL"));
        mvc.perform(post("/v1/ordenes-compra/" + ordenId + "/recepciones").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content("{\"lineas\":[{\"articuloSku\":\"TORN-1\",\"cantidad\":6}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estatus").value("RECIBIDA"));

        // Cancelación sin motivo: 400; orden nueva cancelada con motivo: 200
        MvcResult otra = mvc.perform(post("/v1/ordenes-compra").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content(ordenJson(proveedor)))
                .andReturn();
        String otraId = JsonPath.read(otra.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/v1/ordenes-compra/" + otraId + "/cancelacion").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/v1/ordenes-compra/" + otraId + "/cancelacion").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content("{\"motivo\":\"Proveedor sin stock\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("CANCELADA"));
    }

    private static String ordenJson(String proveedor) {
        return "{\"proveedorId\":\"" + proveedor + "\",\"moneda\":\"MXN\",\"lineas\":["
                + "{\"articuloSku\":\"TORN-1\",\"cantidad\":10,\"precioUnitario\":100}]}";
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
