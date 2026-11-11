package com.mx.josesaid.develop.inventario.domain;

import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar artículos, sin depender de JPA. */
public interface ArticuloRepositorio {
    Articulo guardar(Articulo articulo);
    Optional<Articulo> buscarPorId(UUID id);
    boolean existePorSku(String sku);
    Optional<Articulo> buscarPorSku(String sku);

    /** Artículos con mínimo mayor a cero. */
    java.util.List<Articulo> listarConMinimo();
}
