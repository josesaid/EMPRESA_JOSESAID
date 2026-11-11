package com.mx.josesaid.develop.inventario.domain;

import java.util.List;

/** Puerto para guardar y consultar movimientos, sin depender de JPA. */
public interface MovimientoRepositorio {
    MovimientoInventario guardar(MovimientoInventario movimiento);
    List<MovimientoInventario> buscarPorLote(String lote);

    /** Movimientos más recientes primero, hasta {@code limite}. */
    List<MovimientoInventario> listarRecientesDeArticulo(java.util.UUID articuloId, int limite);
}
