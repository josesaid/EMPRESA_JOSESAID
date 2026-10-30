package com.mx.josesaid.develop.inventario.infrastructure.indicadores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jayway.jsonpath.JsonPath;
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
@SpringBootTest(properties = "inventario.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@Testcontainers
class PublicadorIndicadoresMovimientosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresMovimientos publicador;

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    @Test
    void movimientosDelMesCuentaTodosLosDelPeriodo() {
        String periodo = YearMonth.now(ZONA).toString();
        UUID articulo = UUID.randomUUID();
        UUID almacen = UUID.randomUUID();
        jdbc.update("INSERT INTO articulos (id, sku, nombre, unidad, lote_obligatorio, minimo) VALUES (?, ?, 'Tornillo', 'PZA', FALSE, 0)",
                articulo, "SKU-" + articulo.toString().substring(0, 8));
        jdbc.update("INSERT INTO almacenes (id, nombre, ubicacion) VALUES (?, 'Central', 'CDMX')", almacen);
        movimiento(articulo, almacen, "ENTRADA");
        movimiento(articulo, almacen, "SALIDA");
        movimiento(articulo, almacen, "RESERVA");

        publicador.publicarPeriodo(periodo);

        String clave = PublicadorIndicadoresMovimientos.AREA + "|" + PublicadorIndicadoresMovimientos.NOMBRE + "|" + periodo;
        String payload = leerIndicador(clave);
        assertEquals("OPERACIONES", JsonPath.read(payload, "$.area"));
        assertEquals(3.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);
    }

    private void movimiento(UUID articulo, UUID almacen, String tipo) {
        jdbc.update("""
                INSERT INTO movimientos_inventario (id, articulo_id, almacen_id, tipo, cantidad, creado_utc)
                VALUES (?, ?, ?, ?, 1.000, now())
                """, UUID.randomUUID(), articulo, almacen, tipo);
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
