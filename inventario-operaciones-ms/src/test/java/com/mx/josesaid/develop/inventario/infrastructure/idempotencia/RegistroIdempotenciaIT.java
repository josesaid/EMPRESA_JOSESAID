package com.mx.josesaid.develop.inventario.infrastructure.idempotencia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/** Reserva de Idempotency-Key contra PostgreSQL real (Testcontainers). Requiere Docker; corre con 'mvn verify'. */
@Testcontainers
class RegistroIdempotenciaIT {

    @Container
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    private static final Instant T0 = Instant.parse("2026-11-05T12:00:00Z");

    private JdbcTemplate jdbc;

    @BeforeEach
    void migrarBaseLimpia() {
        DriverManagerDataSource ds = new DriverManagerDataSource(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        Flyway flyway = Flyway.configure().dataSource(ds).cleanDisabled(false).load();
        flyway.clean();
        flyway.migrate();
        jdbc = new JdbcTemplate(ds);
    }

    private RegistroIdempotencia registroEn(Instant instante) {
        return new RegistroIdempotencia(jdbc, Clock.fixed(instante, ZoneOffset.UTC));
    }

    @Test
    void reservaFrescaNoSeRetoma() {
        assertTrue(registroEn(T0).reservar("k1", "h1"));
        assertFalse(registroEn(T0.plusSeconds(60)).reservar("k1", "h1"));
    }

    @Test
    void reservaSinCompletarSeRetomaTrasLaConcesion() {
        assertTrue(registroEn(T0).reservar("k1", "h1"));
        assertTrue(registroEn(T0.plusSeconds(180)).reservar("k1", "h2"));
        assertEquals("h2", registroEn(T0.plusSeconds(180)).buscar("k1").orElseThrow().huella());
        assertFalse(registroEn(T0.plusSeconds(200)).reservar("k1", "h2"));
    }

    @Test
    void respuestaCompletadaNoSeRetomaAunquePaseLaConcesion() {
        assertTrue(registroEn(T0).reservar("k1", "h1"));
        registroEn(T0).completar("k1", 201, "{}");
        assertFalse(registroEn(T0.plusSeconds(180)).reservar("k1", "h1"));
        assertTrue(registroEn(T0.plusSeconds(180)).buscar("k1").orElseThrow().completada());
    }
}
