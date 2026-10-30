package com.mx.josesaid.develop.compras.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrdenCompraRepositorio {
    OrdenCompra guardar(OrdenCompra orden);
    Optional<OrdenCompra> buscarPorId(UUID id);
}
