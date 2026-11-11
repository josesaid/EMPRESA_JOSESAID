package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pago a proveedor visto desde el listado: sin el detalle de la cuenta.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param cuentaId identificador de la cuenta contable, de tipo {@code UUID}.
 * @param monto monto del movimiento, de tipo {@code BigDecimal}.
 * @param fechaPago fecha en que se realizó el pago, de tipo {@code LocalDate}.
 * @param referencia referencia externa o interna del documento, de tipo {@code String}.
 */
public record PagoResumen(UUID id, UUID cuentaId, BigDecimal monto, LocalDate fechaPago, String referencia) {
}
