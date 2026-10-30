package com.mx.josesaid.develop.compras.infrastructure.indicadores;

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
@SpringBootTest(properties = {
        "compras.jwt.jwks-uri=http://localhost:9/.well-known/jwks.json",
        "compras.cifrado.clave=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "compras.cifrado.indice-clave=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="})
@Testcontainers
class PublicadorIndicadoresOrdenesIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresOrdenes publicador;

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    @Test
    void ordenesDelMesCuentanTodasLasCreadasConFecha() {
        String periodo = YearMonth.now(ZONA).toString();
        UUID proveedor = proveedor();
        orden(proveedor, "BORRADOR");
        orden(proveedor, "APROBADA");
        orden(proveedor, "CANCELADA");
        ordenSinFecha(proveedor, "APROBADA");  // excluida: sin fecha; V4 solo asigna fecha a las órdenes existentes al migrar

        publicador.publicarPeriodo(periodo);

        String payload = leerIndicador(PublicadorIndicadoresOrdenes.AREA + "|" + PublicadorIndicadoresOrdenes.NOMBRE + "|" + periodo);
        assertEquals("COMPRAS_PROVEEDORES", JsonPath.read(payload, "$.area"));
        assertEquals(3.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);

        String canceladas = leerIndicador(PublicadorIndicadoresOrdenes.AREA + "|" + PublicadorIndicadoresOrdenes.NOMBRE_CANCELADAS + "|" + periodo);
        assertEquals(1.0, ((Number) JsonPath.read(canceladas, "$.valor")).doubleValue(), 0.001);
    }

    private UUID proveedor() {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO proveedores (id, razon_social, rfc_cifrado, rfc_indice, correo_cifrado, estatus, version)
                VALUES (?, 'Proveedor SA', 'cifrado', ?, 'cifrado', 'ACTIVO', 0)
                """, id, UUID.randomUUID().toString());
        return id;
    }

    /** Usa el DEFAULT now() de la columna, como ocurre en el servicio. */
    private void orden(UUID proveedor, String estatus) {
        jdbc.update("""
                INSERT INTO ordenes_compra (id, proveedor_id, moneda, estatus, version)
                VALUES (?, ?, 'MXN', ?, 0)
                """, UUID.randomUUID(), proveedor, estatus);
    }

    private void ordenSinFecha(UUID proveedor, String estatus) {
        jdbc.update("""
                INSERT INTO ordenes_compra (id, proveedor_id, moneda, estatus, version, creado_utc)
                VALUES (?, ?, 'MXN', ?, 0, NULL)
                """, UUID.randomUUID(), proveedor, estatus);
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
