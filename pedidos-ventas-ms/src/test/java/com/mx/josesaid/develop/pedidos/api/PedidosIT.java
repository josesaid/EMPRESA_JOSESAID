package com.mx.josesaid.develop.pedidos.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.mx.josesaid.develop.pedidos.application.ValidadorCliente;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import java.util.UUID;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integración con PostgreSQL y Kafka reales (Testcontainers). Requiere Docker; corre con 'mvn verify'. */
@SpringBootTest(properties = "pedidos.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@AutoConfigureMockMvc
@Testcontainers
class PedidosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private MockMvc mvc;

    /** Sustituye la consulta a clientes-ms: el adaptador se prueba aparte con un servidor REST simulado. */
    @MockitoBean
    private ValidadorCliente validadorCliente;

    private static final String CLIENTE = "11111111-1111-1111-1111-111111111111";
    private static final String PRODUCTO = "22222222-2222-2222-2222-222222222222";

    @Test
    void flujoCompletoDeCotizacionAPedidoYDevolucion() throws Exception {
        String cuerpo = """
                {"clienteId":"%s","canal":"WEB","moneda":"MXN","tasaIva":"IVA_GENERAL",
                 "vigenteHasta":"2030-12-31",
                 "lineas":[{"productoId":"%s","descripcion":"Tornillo","cantidad":2,"precioUnitario":10.00}]}
                """.formatted(CLIENTE, PRODUCTO);

        MvcResult creada = mvc.perform(post("/v1/cotizaciones").with(rol("VENTAS"))
                        .contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.totales.total").value(23.2))
                .andReturn();
        String cot = JsonPath.read(creada.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/v1/cotizaciones/" + cot + "/envio").with(rol("VENTAS")))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/cotizaciones/" + cot + "/aceptacion").with(rol("VENTAS")))
                .andExpect(status().isOk());

        MvcResult pedido = mvc.perform(post("/v1/cotizaciones/" + cot + "/pedido").with(rol("VENTAS")))
                .andExpect(status().isCreated())
                .andReturn();
        String id = JsonPath.read(pedido.getResponse().getContentAsString(), "$.id");

        // Una cotización solo se convierte una vez
        mvc.perform(post("/v1/cotizaciones/" + cot + "/pedido").with(rol("VENTAS")))
                .andExpect(status().isConflict());

        mvc.perform(post("/v1/pedidos/" + id + "/confirmacion").with(rol("VENTAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("CONFIRMADO"));
        mvc.perform(post("/v1/pedidos/" + id + "/entrega").with(rol("ALMACEN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("ENTREGADO"));
        mvc.perform(post("/v1/pedidos/" + id + "/devolucion").with(rol("VENTAS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estatus").value("CANCELADO"))
                .andExpect(jsonPath("$.motivoCancelacion").value("DEVOLUCION_TOTAL"));
        mvc.perform(post("/v1/pedidos/" + id + "/cancelacion").with(rol("VENTAS"))
                        .contentType("application/json").content("{\"motivo\":\"ERROR_CAPTURA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void cotizacionVencidaResponde410YNoSeReactiva() throws Exception {
        String cuerpo = """
                {"clienteId":"%s","canal":"TELEFONO","moneda":"USD","tasaIva":"IVA_EXENTO",
                 "vigenteHasta":"2020-01-01",
                 "lineas":[{"productoId":"%s","cantidad":1,"precioUnitario":50}]}
                """.formatted(CLIENTE, PRODUCTO);
        MvcResult creada = mvc.perform(post("/v1/cotizaciones").with(rol("VENTAS"))
                        .contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        String cot = JsonPath.read(creada.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/v1/cotizaciones/" + cot + "/envio").with(rol("VENTAS")))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/cotizaciones/" + cot + "/aceptacion").with(rol("VENTAS")))
                .andExpect(status().isGone());
        mvc.perform(post("/v1/cotizaciones/" + cot + "/aceptacion").with(rol("VENTAS")))
                .andExpect(status().isGone());
        mvc.perform(get("/v1/cotizaciones/" + cot).with(rol("VENTAS")))
                .andExpect(jsonPath("$.estatus").value("VENCIDA"));
    }

    @Test
    void rolesYValidacionesSeAplican() throws Exception {
        String cuerpo = """
                {"clienteId":"%s","canal":"WEB","moneda":"MXN","tasaIva":"IVA_GENERAL",
                 "lineas":[{"productoId":"%s","cantidad":1,"precioUnitario":5}]}
                """.formatted(CLIENTE, PRODUCTO);
        MvcResult creado = mvc.perform(post("/v1/pedidos").with(rol("VENTAS"))
                        .contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        String id = JsonPath.read(creado.getResponse().getContentAsString(), "$.id");

        // Solo ALMACEN entrega: VENTAS recibe 403
        mvc.perform(post("/v1/pedidos/" + id + "/entrega").with(rol("VENTAS")))
                .andExpect(status().isForbidden());

        // Sin líneas no se confirma: 422
        mvc.perform(put("/v1/pedidos/" + id + "/lineas").with(rol("VENTAS"))
                        .contentType("application/json").content("{\"lineas\":[]}"))
                .andExpect(status().isOk());
        mvc.perform(post("/v1/pedidos/" + id + "/confirmacion").with(rol("VENTAS")))
                .andExpect(status().isUnprocessableEntity());

        // Métricas solo para DIRECCION o FINANZAS
        mvc.perform(get("/v1/metricas/ventas?mes=2026-10").with(rol("VENTAS")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/v1/metricas/ventas?mes=2026-10").with(rol("DIRECCION")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mes").value("2026-10"));
    }

    private static RequestPostProcessor rol(String rol) {
        return jwt().authorities(new SimpleGrantedAuthority("ROLE_" + rol));
    }

    @Test
    void mismaIdempotencyKeyNoDuplicaElPedido() throws Exception {
        String cuerpo = """
                {"clienteId":"%s","canal":"WEB","moneda":"MXN","tasaIva":"IVA_GENERAL",
                 "lineas":[{"productoId":"%s","descripcion":"Tornillo","cantidad":2,"precioUnitario":10.00}]}
                """.formatted(CLIENTE, PRODUCTO);
        String clave = "prueba-" + UUID.randomUUID();

        MvcResult primero = mvc.perform(post("/v1/pedidos").with(rol("VENTAS"))
                        .header("Idempotency-Key", clave).contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        MvcResult reintento = mvc.perform(post("/v1/pedidos").with(rol("VENTAS"))
                        .header("Idempotency-Key", clave).contentType("application/json").content(cuerpo))
                .andExpect(status().isCreated()).andReturn();
        assertEquals((String) JsonPath.read(primero.getResponse().getContentAsString(), "$.id"),
                (String) JsonPath.read(reintento.getResponse().getContentAsString(), "$.id"));

        // Misma clave con otra petición: 422
        mvc.perform(post("/v1/pedidos").with(rol("VENTAS"))
                        .header("Idempotency-Key", clave).contentType("application/json")
                        .content(cuerpo.replace("\"WEB\"", "\"TIENDA\"")))
                .andExpect(status().isUnprocessableEntity());
    }
}
