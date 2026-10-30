package com.mx.josesaid.develop.clientes.infrastructure.indicadores;

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
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Integración con PostgreSQL y Kafka reales (Testcontainers). Requiere Docker; corre con 'mvn verify'. */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PublicadorIndicadoresClientesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresClientes publicador;

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    @Test
    void clientesNuevosDelMesCuentaSoloLasAltasDelPeriodo() {
        LocalDate hoy = LocalDate.now(ZONA);
        String periodo = YearMonth.from(hoy).toString();
        cliente(hoy, "ACTIVO");
        cliente(hoy, "SUSPENDIDO");
        cliente(hoy.minusMonths(1), "ACTIVO");  // excluido: alta del mes anterior

        publicador.publicarPeriodo(periodo);

        String clave = PublicadorIndicadoresClientes.AREA + "|" + PublicadorIndicadoresClientes.NOMBRE + "|" + periodo;
        String payload = leerIndicador(clave);
        assertEquals("VENTAS_ATENCION_CLIENTES", JsonPath.read(payload, "$.area"));
        assertEquals(2.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);
    }

    private void cliente(LocalDate fechaAlta, String estatus) {
        jdbc.update("""
                INSERT INTO cliente (id, razon_social, rfc_cifrado, rfc_hash, estatus, fecha_alta,
                                     created_at, updated_at, version)
                VALUES (?, 'Cliente SA', 'cifrado', ?, ?, ?, now(), now(), 0)
                """, UUID.randomUUID(), UUID.randomUUID().toString(), estatus,
                java.sql.Date.valueOf(fechaAlta));
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
