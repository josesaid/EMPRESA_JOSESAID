package com.mx.josesaid.develop.inventario.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar órdenes, sin depender de JPA. */
public interface OrdenRepositorio {
    OrdenOperacion guardar(OrdenOperacion orden);
    Optional<OrdenOperacion> buscarPorId(UUID id);
}
