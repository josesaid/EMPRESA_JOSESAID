package com.mx.josesaid.develop.inventario.domain;

import java.util.Optional;
import java.util.UUID;

public interface ArticuloRepositorio {
    Articulo guardar(Articulo articulo);
    Optional<Articulo> buscarPorId(UUID id);
    boolean existePorSku(String sku);
}
