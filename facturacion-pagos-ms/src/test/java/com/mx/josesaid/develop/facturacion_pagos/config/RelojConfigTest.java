package com.mx.josesaid.develop.facturacion_pagos.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RelojConfigTest {

    @Test
    void relojUsaLaZonaDeOperacion() {
        assertEquals(ZoneId.of("America/Mexico_City"), new RelojConfig().reloj().getZone());
    }
}
