package com.mx.josesaid.develop.compras.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Registro de una recepción. Es histórico: no se modifica ni se borra.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param ordenId identificador del orden, de tipo {@code UUID}.
 * @param articuloSku articulo sku, de tipo {@code String}.
 * @param cantidad cantidad solicitada o registrada, de tipo {@code BigDecimal}.
 */
public record Recepcion(UUID id, UUID ordenId, String articuloSku, BigDecimal cantidad) {
}
