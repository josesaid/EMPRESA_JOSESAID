package com.mx.josesaid.develop.inventario.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExistenciaRepositorio {
    Existencia guardar(Existencia existencia);
    Optional<Existencia> buscar(UUID articuloId, UUID almacenId);
    List<Existencia> buscarPorArticulo(UUID articuloId);
}
