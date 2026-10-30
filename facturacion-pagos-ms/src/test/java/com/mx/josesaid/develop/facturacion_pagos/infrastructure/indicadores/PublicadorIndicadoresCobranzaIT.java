package com.mx.josesaid.develop.facturacion_pagos.infrastructure.indicadores;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
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
@SpringBootTest(properties = "facturacion.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json")
@Testcontainers
class PublicadorIndicadoresCobranzaIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresCobranza publicador;

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    @Test
    void cobradoDelMesSumaPagosMxnDeComprobantesEmitidos() {
        String periodo = YearMonth.now(ZONA).toString();
        UUID emitido = comprobante("EMITIDO", "MXN");
        pago(emitido, "50.00");
        pago(emitido, "30.00");
        pago(comprobante("BORRADOR", "MXN"), "100.00");  // excluido: comprobante en borrador
        pago(comprobante("EMITIDO", "USD"), "40.00");    // excluido: no es MXN

        publicador.publicarPeriodo(periodo);

        String clave = PublicadorIndicadoresCobranza.AREA + "|" + PublicadorIndicadoresCobranza.NOMBRE + "|" + periodo;
        String payload = leerIndicador(clave);
        assertEquals("FINANZAS_CONTABILIDAD", JsonPath.read(payload, "$.area"));
        assertEquals(80.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);
    }

    private UUID comprobante(String estatus, String moneda) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO comprobantes (id, orden_id, moneda, subtotal, iva, total, tasa_iva, estatus,
                                          version, creado_utc, actualizado_utc)
                VALUES (?, ?, ?, 100.00, 16.00, 116.00, 0.16, ?, 0, now(), now())
                """, id, UUID.randomUUID(), moneda, estatus);
        return id;
    }

    private void pago(UUID comprobanteId, String monto) {
        jdbc.update("""
                INSERT INTO pagos (id, comprobante_id, monto, fecha_pago, creado_utc)
                VALUES (?, ?, ?, ?, now())
                """, UUID.randomUUID(), comprobanteId, new BigDecimal(monto),
                java.sql.Date.valueOf(LocalDate.now(ZONA)));
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
