package com.mx.josesaid.develop.compras.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar órdenes de compra, sin depender de JPA. */
public interface OrdenCompraRepositorio {
    OrdenCompra guardar(OrdenCompra orden);
    Optional<OrdenCompra> buscarPorId(UUID id);
    /** Órdenes más recientes primero; estatus opcional (null = todos). */
    List<ResumenOrdenCompra> listarRecientes(EstatusOrdenCompra estatus, int limite);
}
