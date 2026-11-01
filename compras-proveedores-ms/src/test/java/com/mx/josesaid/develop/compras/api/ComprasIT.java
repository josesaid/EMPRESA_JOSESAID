package com.mx.josesaid.develop.compras.api;

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
@SpringBootTest(properties = {
        "compras.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json",
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

        MvcResult orden = mvc.perform(post("/v1/ordenes-compra").with(usuario("compras-1", "COMPRAS"))
                        .contentType("application/json")
                        .content(ordenJson(proveedor)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estatus").value("BORRADOR"))
                .andExpect(jsonPath("$.total").value(1000))
                .andReturn();
        String ordenId = JsonPath.read(orden.getResponse().getContentAsString(), "$.id");

        // El creador no aprueba su propia orden, aunque sea COMPRAS
        mvc.perform(post("/v1/ordenes-compra/" + ordenId + "/aprobacion").with(usuario("compras-1", "COMPRAS")))
                .andExpect(status().isForbidden());
        // DIRECCION aprueba una orden que creó otro usuario
        mvc.perform(post("/v1/ordenes-compra/" + ordenId + "/aprobacion").with(usuario("direccion-1", "DIRECCION")))
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

    private static RequestPostProcessor usuario(String sub, String rol) {
        return jwt().jwt(j -> j.subject(sub)).authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void buscaProveedoresPorNombreSinExponerRfcNiCorreoEnClaro() throws Exception {
        mvc.perform(post("/v1/proveedores").with(rol("COMPRAS"))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"razonSocial\":\"Busqueda Prueba SA\",\"rfc\":\"PBU010101AB1\",\"correo\":\"ventas@busqueda.mx\"}"))
                .andExpect(status().isCreated());

        mvc.perform(get("/v1/proveedores").param("razonSocial", "busqueda prue").with(rol("FINANZAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].razonSocial").value("Busqueda Prueba SA"))
                .andExpect(jsonPath("$[0].correo").value("v***@busqueda.mx"));

        mvc.perform(get("/v1/proveedores").param("razonSocial", "bu").with(rol("COMPRAS")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mismaIdempotencyKeyNoDuplicaLaOrden() throws Exception {
        MvcResult prov = mvc.perform(post("/v1/proveedores").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content("{\"razonSocial\":\"Replay SA\",\"rfc\":\"BBN010101AB3\",\"correo\":\"replay@aceros.mx\"}"))
                .andExpect(status().isCreated()).andReturn();
        String proveedor = JsonPath.read(prov.getResponse().getContentAsString(), "$.id");

        String clave = "prueba-" + java.util.UUID.randomUUID();
        MvcResult primero = mvc.perform(post("/v1/ordenes-compra").with(rol("COMPRAS")).header("Idempotency-Key", clave)
                        .contentType("application/json").content(ordenJson(proveedor)))
                .andExpect(status().isCreated()).andReturn();
        MvcResult reintento = mvc.perform(post("/v1/ordenes-compra").with(rol("COMPRAS")).header("Idempotency-Key", clave)
                        .contentType("application/json").content(ordenJson(proveedor)))
                .andExpect(status().isCreated()).andReturn();
        assertEquals((String) JsonPath.read(primero.getResponse().getContentAsString(), "$.id"),
                (String) JsonPath.read(reintento.getResponse().getContentAsString(), "$.id"));
    }
}
