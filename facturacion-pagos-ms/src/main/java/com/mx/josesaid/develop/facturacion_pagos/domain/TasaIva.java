package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Tasa de IVA según la fecha de emisión. Desde el 1 de noviembre de 2026 aplica la tasa fronteriza
 * del 8 % a todas las operaciones; antes, la tasa general del 16 %. La fija el servidor: el cliente
 * no la envía.
 */
public final class TasaIva {

    public static final LocalDate INICIO_FRONTERIZO = LocalDate.of(2026, 11, 1);
    static final BigDecimal GENERAL = new BigDecimal("0.16");
    static final BigDecimal FRONTERIZO = new BigDecimal("0.08");

    /** Clase de utilidad; no se instancia. */
    private TasaIva() {
    }

    /** Devuelve la tasa de IVA vigente para la fecha: 16 % antes del 1 de noviembre de 2026 y 8 % desde esa fecha. */
    public static BigDecimal paraFecha(LocalDate fecha) {
        return fecha.isBefore(INICIO_FRONTERIZO) ? GENERAL : FRONTERIZO;
    }
}
