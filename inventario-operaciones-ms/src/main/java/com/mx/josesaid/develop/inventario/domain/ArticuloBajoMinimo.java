package com.mx.josesaid.develop.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Artículo cuya existencia total está por debajo de su mínimo. Faltante = mínimo menos existencia.
 *
 * @param articuloId componente de tipo {@code UUID}.
 * @param sku componente de tipo {@code String}.
 * @param nombre componente de tipo {@code String}.
 * @param unidad componente de tipo {@code String}.
 * @param minimo componente de tipo {@code BigDecimal}.
 * @param existencia componente de tipo {@code BigDecimal}.
 * @param faltante componente de tipo {@code BigDecimal}.
 */
public record ArticuloBajoMinimo(UUID articuloId, String sku, String nombre, String unidad, BigDecimal minimo,
                                 BigDecimal existencia, BigDecimal faltante) {
}
