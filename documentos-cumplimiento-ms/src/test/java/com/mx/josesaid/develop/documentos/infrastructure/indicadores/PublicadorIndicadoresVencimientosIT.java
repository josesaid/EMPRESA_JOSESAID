package com.mx.josesaid.develop.documentos.infrastructure.indicadores;

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
@SpringBootTest(properties = {
        "documentos.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json",
        "documentos.cifrado.clave=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "documentos.almacen.directorio=${java.io.tmpdir}/documentos-it-${random.uuid}"})
@Testcontainers
class PublicadorIndicadoresVencimientosIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresVencimientos publicador;

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    @Test
    void documentosPorVencerCuentaSoloVigentesDentroDe30Dias() {
        LocalDate hoy = LocalDate.now(ZONA);
        String periodo = YearMonth.from(hoy).toString();
        documento(hoy.plusDays(10), false, null);            // cuenta
        documento(hoy.plusDays(60), false, null);            // fuera de rango
        documento(hoy.plusDays(5), true, "Duplicado");       // excluido: eliminado
        documento(hoy.minusDays(1), false, null);            // excluido: ya venció

        publicador.publicar();

        String clave = PublicadorIndicadoresVencimientos.AREA + "|" + PublicadorIndicadoresVencimientos.NOMBRE + "|" + periodo;
        String payload = leerIndicador(clave);
        assertEquals("ADMINISTRACION_LEGAL", JsonPath.read(payload, "$.area"));
        assertEquals(1.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);
    }

    private void documento(LocalDate venceEn, boolean eliminado, String motivo) {
        jdbc.update("""
                INSERT INTO documentos (id, tipo, titulo, area, vence_en, retencion_hasta, eliminado,
                                        motivo_eliminacion, version)
                VALUES (?, 'CONTRATO', 'Contrato', 'Legal', ?, ?, ?, ?, 0)
                """, UUID.randomUUID(), java.sql.Date.valueOf(venceEn),
                java.sql.Date.valueOf(LocalDate.now(ZONA).plusYears(10)), eliminado, motivo);
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
