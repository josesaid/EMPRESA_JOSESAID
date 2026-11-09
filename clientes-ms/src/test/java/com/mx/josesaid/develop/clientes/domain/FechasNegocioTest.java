package com.mx.josesaid.develop.clientes.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class FechasNegocioTest {

    @Test
    void usaLaFechaDeMexicoAunqueEnUtcYaSeaOtroDia() {
        // 2026-11-08 22:34 hora de México = 2026-11-09 04:34 UTC
        Clock reloj = Clock.fixed(Instant.parse("2026-11-09T04:34:00Z"), ZoneOffset.UTC);

        assertEquals(LocalDate.of(2026, 11, 8), FechasNegocio.hoy(reloj));
    }
}
