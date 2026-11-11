package com.mx.josesaid.develop.compras.application;

import java.math.BigDecimal;

/**
 * Una línea de una recepción: SKU y cantidad recibida.
 *
 * @param sku clave SKU del artículo, de tipo {@code String}.
 * @param cantidad cantidad solicitada o registrada, de tipo {@code BigDecimal}.
 */
public record LineaRecibida(String sku, BigDecimal cantidad) {
}
