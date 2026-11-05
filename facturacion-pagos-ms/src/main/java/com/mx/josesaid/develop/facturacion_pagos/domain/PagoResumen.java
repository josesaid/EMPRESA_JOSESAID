package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Pago a proveedor visto desde el listado: sin el detalle de la cuenta. */
public record PagoResumen(UUID id, UUID cuentaId, BigDecimal monto, LocalDate fechaPago, String referencia) {
}
