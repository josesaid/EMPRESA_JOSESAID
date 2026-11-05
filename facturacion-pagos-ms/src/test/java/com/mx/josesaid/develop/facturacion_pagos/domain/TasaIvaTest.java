package com.mx.josesaid.develop.facturacion_pagos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class TasaIvaTest {

    @Test
    void antesDelPrimeroDeNoviembreEs16() {
        assertThat(TasaIva.paraFecha(LocalDate.of(2026, 10, 31))).isEqualByComparingTo("0.16");
    }

    @Test
    void desdeElPrimeroDeNoviembreEs8() {
        assertThat(TasaIva.paraFecha(LocalDate.of(2026, 11, 1))).isEqualByComparingTo("0.08");
    }

    @Test
    void comprobanteUsaLaTasaDeSuFecha() {
        Comprobante c = Comprobante.crearBorrador(UUID.randomUUID(), "MXN", new BigDecimal("1000"),
                LocalDate.of(2026, 11, 5));
        assertThat(c.getTasaIva()).isEqualByComparingTo("0.08");
        assertThat(c.getIva()).isEqualByComparingTo("80.00");
        assertThat(c.getTotal()).isEqualByComparingTo("1080.00");
    }
}
