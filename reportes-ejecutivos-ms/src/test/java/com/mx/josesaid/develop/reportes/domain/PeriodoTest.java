package com.mx.josesaid.develop.reportes.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class PeriodoTest {

    @Test
    void aceptaMensualTrimestralYAnual() {
        Periodo.de("2026-10");
        Periodo.de("2026-Q4");
        Periodo.de("2026");
    }

    @Test
    void rechazaFormatosInvalidos() {
        assertThrows(SolicitudInvalidaException.class, () -> Periodo.de("2026-13"));
        assertThrows(SolicitudInvalidaException.class, () -> Periodo.de("2026-Q5"));
        assertThrows(SolicitudInvalidaException.class, () -> Periodo.de("octubre"));
    }
}
