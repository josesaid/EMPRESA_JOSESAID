package com.mx.josesaid.develop.usuarios_accesos.infrastructure.indicadores;

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
@SpringBootTest
@Testcontainers
class PublicadorIndicadoresUsuariosIT {

    /** Claves de prueba: la clave RSA se genera en cada ejecución; las de cifrado son fijas solo para pruebas. */
    @org.springframework.test.context.DynamicPropertySource
    static void clavesDePrueba(org.springframework.test.context.DynamicPropertyRegistry registro) throws Exception {
        java.security.KeyPairGenerator generador = java.security.KeyPairGenerator.getInstance("RSA");
        generador.initialize(2048);
        String pkcs8 = java.util.Base64.getEncoder().encodeToString(generador.generateKeyPair().getPrivate().getEncoded());
        registro.add("usuarios.jwt.clave-privada", () -> pkcs8);
        registro.add("USUARIOS_CIFRADO_CLAVE", () -> java.util.Base64.getEncoder()
                .encodeToString("0123456789abcdef0123456789abcdef".getBytes()));
        registro.add("USUARIOS_INDICE_CLAVE", () -> java.util.Base64.getEncoder()
                .encodeToString("fedcba9876543210fedcba9876543210".getBytes()));
    }

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Container
    @ServiceConnection
    static KafkaContainer kafka = new KafkaContainer("apache/kafka:4.0.0");

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PublicadorIndicadoresUsuarios publicador;

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    @Test
    void usuariosActivosCuentaSoloElEstatusActivo() {
        String periodo = YearMonth.now(ZONA).toString();
        // El arranque puede crear un administrador inicial ACTIVO; se cuenta la base existente.
        long base = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE estatus = 'ACTIVO'", Long.class);
        usuario("ACTIVO");
        usuario("ACTIVO");
        usuario("BLOQUEADO");  // excluido
        usuario("BAJA");       // excluido

        publicador.publicar();

        String clave = PublicadorIndicadoresUsuarios.AREA + "|" + PublicadorIndicadoresUsuarios.NOMBRE + "|" + periodo;
        String payload = leerIndicador(clave);
        assertEquals("ADMINISTRACION_LEGAL", JsonPath.read(payload, "$.area"));
        assertEquals(base + 2.0, ((Number) JsonPath.read(payload, "$.valor")).doubleValue(), 0.001);
    }

    private void usuario(String estatus) {
        jdbc.update("""
                INSERT INTO usuarios (id, correo, nombre_completo, password_hash, estatus, creado_utc, actualizado_utc)
                VALUES (?, ?, 'Usuario de prueba', 'hash', ?, now(), now())
                """, UUID.randomUUID(), UUID.randomUUID() + "@prueba.mx", estatus);
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
