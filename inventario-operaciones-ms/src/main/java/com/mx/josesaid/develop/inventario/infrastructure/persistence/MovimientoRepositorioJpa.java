package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.MovimientoRepositorio;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
class MovimientoRepositorioJpa implements MovimientoRepositorio {

    private final MovimientoJpaRepository jpa;

    MovimientoRepositorioJpa(MovimientoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public MovimientoInventario guardar(MovimientoInventario m) {
        MovimientoInventarioEntity e = new MovimientoInventarioEntity();
        e.id = m.getId();
        e.articuloId = m.getArticuloId();
        e.almacenId = m.getAlmacenId();
        e.tipo = m.getTipo();
        e.cantidad = m.getCantidad();
        e.lote = m.getLote();
        e.referencia = m.getReferencia();
        e.creadoUtc = m.getCreadoUtc();
        jpa.save(e);
        return m;
    }

    @Override
    public List<MovimientoInventario> buscarPorLote(String lote) {
        return jpa.findByLoteOrderByCreadoUtcAsc(lote).stream()
                .map(e -> MovimientoInventario.reconstruir(e.id, e.articuloId, e.almacenId, e.tipo, e.cantidad,
                        e.lote, e.referencia, e.creadoUtc))
                .toList();
    }
}
