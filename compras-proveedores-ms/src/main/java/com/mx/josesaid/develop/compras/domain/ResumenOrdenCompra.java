package com.mx.josesaid.develop.compras.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos de una orden que el listado necesita: sin líneas ni montos.
 *
 * @param id componente de tipo {@code UUID}.
 * @param proveedorId componente de tipo {@code UUID}.
 * @param estatus componente de tipo {@code EstatusOrdenCompra}.
 * @param creadaUtc componente de tipo {@code Instant}.
 */
public record ResumenOrdenCompra(UUID id, UUID proveedorId, EstatusOrdenCompra estatus, Instant creadaUtc) {
}
