package com.mx.josesaid.develop.compras.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Registro de una recepción. Es histórico: no se modifica ni se borra. */
public record Recepcion(UUID id, UUID ordenId, String articuloSku, BigDecimal cantidad) {
}
