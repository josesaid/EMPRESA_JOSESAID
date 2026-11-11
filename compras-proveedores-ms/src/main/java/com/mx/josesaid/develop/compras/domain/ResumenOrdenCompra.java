package com.mx.josesaid.develop.compras.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Datos de una orden que el listado necesita: sin líneas ni montos.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param proveedorId identificador del proveedor, de tipo {@code UUID}.
 * @param estatus estado actual del registro, de tipo {@code EstatusOrdenCompra}.
 * @param creadaUtc creada utc, de tipo {@code Instant}.
 */
public record ResumenOrdenCompra(UUID id, UUID proveedorId, EstatusOrdenCompra estatus, Instant creadaUtc) {
}
