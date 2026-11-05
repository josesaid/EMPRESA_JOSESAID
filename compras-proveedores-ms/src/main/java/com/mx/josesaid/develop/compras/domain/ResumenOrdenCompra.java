package com.mx.josesaid.develop.compras.domain;

import java.time.Instant;
import java.util.UUID;

/** Datos de una orden que el listado necesita: sin líneas ni montos. */
public record ResumenOrdenCompra(UUID id, UUID proveedorId, EstatusOrdenCompra estatus, Instant creadaUtc) {
}
