package com.mx.josesaid.develop.compras.application;

import java.math.BigDecimal;

/** Una línea de una recepción: SKU y cantidad recibida. */
public record LineaRecibida(String sku, BigDecimal cantidad) {
}
