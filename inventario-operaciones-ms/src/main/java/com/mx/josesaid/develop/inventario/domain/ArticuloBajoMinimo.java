package com.mx.josesaid.develop.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Artículo cuya existencia total está por debajo de su mínimo. Faltante = mínimo menos existencia.
 *
 * @param articuloId identificador del artículo, de tipo {@code UUID}.
 * @param sku clave SKU del artículo, de tipo {@code String}.
 * @param nombre nombre descriptivo, de tipo {@code String}.
 * @param unidad unidad, de tipo {@code String}.
 * @param minimo minimo, de tipo {@code BigDecimal}.
 * @param existencia existencia, de tipo {@code BigDecimal}.
 * @param faltante faltante, de tipo {@code BigDecimal}.
 */
public record ArticuloBajoMinimo(UUID articuloId, String sku, String nombre, String unidad, BigDecimal minimo,
                                 BigDecimal existencia, BigDecimal faltante) {
}
