package com.mx.josesaid.develop.pedidos.infrastructure.indicadores;

import com.mx.josesaid.develop.pedidos.application.ValidadorCliente;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Properties;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integración con PostgreSQL y Kafka reales (Testcontainers). Requiere Docker; corre con 'mvn verify'. */
@SpringBootTest(properties = "pedidos.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@Testcontainers
class PublicadorIndicadoresVentasIT {

    @MockitoBean
    ValidadorCliente validadorCliente;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresVentas publicador;

    private static final String PRODUCTO = "22222222-2222-2222-2222-222222222222";

    @Test
    void ventasDelMesIncluyenIvaSoloConfirmadosYEntregadosEnMxn() {
        String periodo = YearMonth.now(ZoneId.of("America/Mexico_City")).toString();
        pedido("CONFIRMADO", "IVA_GENERAL", "MXN", 2, "10.00");    // 23.20
        pedido("ENTREGADO", "IVA_FRONTERIZO", "MXN", 1, "10.00");  // 10.80
        pedido("BORRADOR", "IVA_GENERAL", "MXN", 1, "100.00");     // excluido: borrador
        pedido("CANCELADO", "IVA_GENERAL", "MXN", 1, "100.00");    // excluido: cancelado
        pedido("CONFIRMADO", "IVA_GENERAL", "USD", 1, "100.00");   // excluido: no es MXN

        publicador.publicarPeriodo(periodo);

        String clave = PublicadorIndicadoresVentas.AREA + "|" + PublicadorIndicadoresVentas.NOMBRE + "|" + periodo;
        String payload = leerIndicador(clave);
        assertEquals("VENTAS_ATENCION_CLIENTES", JsonPath.read(payload, "$.area"));
        assertEquals(34.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);
    }

    private void pedido(String estatus, String tasa, String moneda, int cantidad, String precio) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO pedidos (id, cliente_id, canal, moneda, tasa, estatus, fecha_confirmacion, version)
                VALUES (?, ?, 'WEB', ?, ?, ?, now(), 0)
                """, id, UUID.randomUUID(), moneda, tasa, estatus);
        jdbc.update("""
                INSERT INTO lineas_pedido (pedido_id, posicion, producto_id, descripcion, cantidad, precio_unitario)
                VALUES (?, 1, ?, 'Tornillo', ?, ?)
                """, id, UUID.fromString(PRODUCTO), cantidad, new BigDecimal(precio));
    }

    private String leerIndicador(String clave) {
        try (KafkaConsumer<String, String> consumidor = new KafkaConsumer<>(propiedades())) {
            consumidor.subscribe(Collections.singletonList("indicadores-areas"));
            long limite = System.currentTimeMillis() + 30_000;
            while (System.currentTimeMillis() < limite) {
                for (var registro : consumidor.poll(Duration.ofMillis(500))) {
                    if (clave.equals(registro.key())) {
                        return registro.value();
                    }
                }
            }
            throw new AssertionError("No llegó el indicador " + clave);
        }
    }

    private Properties propiedades() {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());
        p.put(ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return p;
    }
}
