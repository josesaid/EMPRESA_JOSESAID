package com.mx.josesaid.develop.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Artículo cuya existencia total está por debajo de su mínimo. Faltante = mínimo menos existencia. */
public record ArticuloBajoMinimo(UUID articuloId, String sku, String nombre, String unidad, BigDecimal minimo,
                                 BigDecimal existencia, BigDecimal faltante) {
}
