package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Comprobante emitido con saldo por cobrar, para el listado de cobranza.
 *
 * @param id componente de tipo {@code UUID}.
 * @param folio componente de tipo {@code String}.
 * @param moneda componente de tipo {@code String}.
 * @param total componente de tipo {@code BigDecimal}.
 * @param saldo componente de tipo {@code BigDecimal}.
 * @param estatusCobro componente de tipo {@code EstatusCobro}.
 */
public record ComprobantePendienteCobro(UUID id, String folio, String moneda, BigDecimal total, BigDecimal saldo,
                                        EstatusCobro estatusCobro) {
}
