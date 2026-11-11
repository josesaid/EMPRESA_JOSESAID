package com.mx.josesaid.develop.inventario.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar almacenes, sin depender de JPA. */
public interface AlmacenRepositorio {
    Almacen guardar(Almacen almacen);
    Optional<Almacen> buscarPorId(UUID id);
}
