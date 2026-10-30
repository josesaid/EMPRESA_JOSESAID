package com.mx.josesaid.develop.contabilidad.infrastructure.indicadores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.LocalDate;
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
@SpringBootTest(properties = "contabilidad.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@Testcontainers
class PublicadorIndicadoresPolizasIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresPolizas publicador;

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    @Test
    void polizasDelMesCuentaSoloLasAplicadas() {
        String periodo = YearMonth.now(ZONA).toString();
        jdbc.update("INSERT INTO periodos (anio_mes, estatus, version) VALUES (?, 'ABIERTO', 0)", periodo);
        poliza(periodo, "APLICADA");
        poliza(periodo, "APLICADA");
        poliza(periodo, "BORRADOR");  // excluida: no aplicada

        publicador.publicarPeriodo(periodo);

        String clave = PublicadorIndicadoresPolizas.AREA + "|" + PublicadorIndicadoresPolizas.NOMBRE + "|" + periodo;
        String payload = leerIndicador(clave);
        assertEquals("FINANZAS_CONTABILIDAD", JsonPath.read(payload, "$.area"));
        assertEquals(2.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);
    }

    private void poliza(String periodo, String estatus) {
        jdbc.update("""
                INSERT INTO polizas (id, anio_mes, tipo, fecha, estatus, version)
                VALUES (?, ?, 'DIARIO', ?, ?, 0)
                """, UUID.randomUUID(), periodo, java.sql.Date.valueOf(LocalDate.now(ZONA)), estatus);
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
