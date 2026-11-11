package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comprobante emitido con saldo por cobrar, para el listado de cobranza.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param folio folio, de tipo {@code String}.
 * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
 * @param total importe total, de tipo {@code BigDecimal}.
 * @param saldo saldo pendiente, de tipo {@code BigDecimal}.
 * @param estatusCobro estatus cobro, de tipo {@code EstatusCobro}.
 */
public record ComprobantePendienteCobro(UUID id, String folio, String moneda, BigDecimal total, BigDecimal saldo,
                                        EstatusCobro estatusCobro) {
}
