package com.mx.josesaid.develop.inventario.api;

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
@SpringBootTest(properties = "inventario.jwt.secreto=clave-de-prueba-de-al-menos-32-bytes-xx")
@AutoConfigureMockMvc
@Testcontainers
class InventarioIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    @Test
    void flujoDeInventarioConLotesReservasYOrdenes() throws Exception {
        mvc.perform(get("/v1/existencias?articulo=00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isUnauthorized());

        // COMPRAS no da de alta almacenes
        mvc.perform(post("/v1/almacenes").with(rol("COMPRAS"))
                        .contentType("application/json")
                        .content("{\"nombre\":\"Central\",\"ubicacion\":\"Planta 1\"}"))
                .andExpect(status().isForbidden());

        String articulo = crearArticulo("SKU-IT-1", true);
        String almacen = crearAlmacen();

        // Artículo con lote obligatorio sin lote: 400
        mvc.perform(post("/v1/movimientos").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content(movimiento(articulo, almacen, "ENTRADA", "20", null)))
                .andExpect(status().isBadRequest());

        // Entrada con lote: 201
        mvc.perform(post("/v1/movimientos").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content(movimiento(articulo, almacen, "ENTRADA", "20", "LOTE-A")))
                .andExpect(status().isCreated());

        // Salida mayor que la existencia: 409
        mvc.perform(post("/v1/movimientos").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content(movimiento(articulo, almacen, "SALIDA", "25", "LOTE-A")))
                .andExpect(status().isConflict());

        // Reserva y consulta de existencias
        mvc.perform(post("/v1/movimientos").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content(movimiento(articulo, almacen, "RESERVA", "5", "LOTE-A")))
                .andExpect(status().isCreated());
        mvc.perform(get("/v1/existencias?articulo=" + articulo).with(rol("VENTAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].disponible").value(15));

        // Trazabilidad del lote
        mvc.perform(get("/v1/lotes/LOTE-A/trazabilidad").with(rol("CALIDAD")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        // Orden de producción: cierre con cantidad real
        MvcResult orden = mvc.perform(post("/v1/ordenes-operacion").with(rol("OPERACIONES"))
                        .contentType("application/json")
                        .content("{\"tipo\":\"PRODUCCION\",\"cantidadObjetivo\":50}"))
                .andExpect(status().isCreated())
                .andReturn();
        String ordenId = JsonPath.read(orden.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/v1/ordenes-operacion/" + ordenId + "/cierre").with(rol("OPERACIONES"))
                        .contentType("application/json")
                        .content("{\"cantidadReal\":48}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("CERRADA"));
    }

    private String crearArticulo(String sku, boolean loteObligatorio) throws Exception {
        MvcResult r = mvc.perform(post("/v1/articulos").with(rol("OPERACIONES"))
                        .contentType("application/json")
                        .content("{\"sku\":\"" + sku + "\",\"nombre\":\"Resina\",\"unidad\":\"KG\","
                                + "\"loteObligatorio\":" + loteObligatorio + ",\"minimo\":10}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private String crearAlmacen() throws Exception {
        MvcResult r = mvc.perform(post("/v1/almacenes").with(rol("ALMACEN"))
                        .contentType("application/json")
                        .content("{\"nombre\":\"Central\",\"ubicacion\":\"Planta 1\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private static String movimiento(String articulo, String almacen, String tipo, String cantidad, String lote) {
        String loteJson = lote == null ? "null" : "\"" + lote + "\"";
        return "{\"articuloId\":\"" + articulo + "\",\"almacenId\":\"" + almacen + "\",\"tipo\":\"" + tipo
                + "\",\"cantidad\":" + cantidad + ",\"lote\":" + loteJson + "}";
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }
}
