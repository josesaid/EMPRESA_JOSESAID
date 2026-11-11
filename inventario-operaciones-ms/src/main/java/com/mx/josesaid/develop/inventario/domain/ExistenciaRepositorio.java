package com.mx.josesaid.develop.inventario.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar existencias, sin depender de JPA. */
public interface ExistenciaRepositorio {
    Existencia guardar(Existencia existencia);
    Optional<Existencia> buscar(UUID articuloId, UUID almacenId);
    List<Existencia> buscarPorArticulo(UUID articuloId);

    /** Suma de cantidad en todos los almacenes, por artículo. */
    java.util.Map<UUID, java.math.BigDecimal> cantidadTotalPorArticulo();
}
