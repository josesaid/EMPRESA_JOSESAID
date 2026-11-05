package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Comprobante emitido con saldo por cobrar, para el listado de cobranza. */
public record ComprobantePendienteCobro(UUID id, String folio, String moneda, BigDecimal total, BigDecimal saldo,
                                        EstatusCobro estatusCobro) {
}
