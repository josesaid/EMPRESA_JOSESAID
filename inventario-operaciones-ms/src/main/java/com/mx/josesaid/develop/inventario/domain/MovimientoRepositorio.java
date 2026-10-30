package com.mx.josesaid.develop.inventario.domain;

import java.util.List;

public interface MovimientoRepositorio {
    MovimientoInventario guardar(MovimientoInventario movimiento);
    List<MovimientoInventario> buscarPorLote(String lote);
}
