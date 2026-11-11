package com.mx.josesaid.develop.compras.application;

import java.math.BigDecimal;

/**
 * Una línea de una recepción: SKU y cantidad recibida.
 *
 * @param sku componente de tipo {@code String}.
 * @param cantidad componente de tipo {@code BigDecimal}.
 */
public record LineaRecibida(String sku, BigDecimal cantidad) {
}
