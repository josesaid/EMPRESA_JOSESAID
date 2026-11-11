package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Pago a proveedor visto desde el listado: sin el detalle de la cuenta.
 *
 * @param id componente de tipo {@code UUID}.
 * @param cuentaId componente de tipo {@code UUID}.
 * @param monto componente de tipo {@code BigDecimal}.
 * @param fechaPago componente de tipo {@code LocalDate}.
 * @param referencia componente de tipo {@code String}.
 */
public record PagoResumen(UUID id, UUID cuentaId, BigDecimal monto, LocalDate fechaPago, String referencia) {
}
