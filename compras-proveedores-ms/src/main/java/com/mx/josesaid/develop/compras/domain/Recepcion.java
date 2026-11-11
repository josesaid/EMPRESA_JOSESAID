package com.mx.josesaid.develop.compras.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Registro de una recepción. Es histórico: no se modifica ni se borra.
 *
 * @param id componente de tipo {@code UUID}.
 * @param ordenId componente de tipo {@code UUID}.
 * @param articuloSku componente de tipo {@code String}.
 * @param cantidad componente de tipo {@code BigDecimal}.
 */
public record Recepcion(UUID id, UUID ordenId, String articuloSku, BigDecimal cantidad) {
}
