package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import com.mx.josesaid.develop.inventario.domain.OrdenOperacion;
import com.mx.josesaid.develop.inventario.domain.OrdenRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OrdenRepositorioJpa implements OrdenRepositorio {

    private final OrdenJpaRepository jpa;

    OrdenRepositorioJpa(OrdenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public OrdenOperacion guardar(OrdenOperacion o) {
        OrdenEntity e = jpa.findById(o.getId()).orElseGet(() -> {
            OrdenEntity nueva = new OrdenEntity();
            nueva.id = o.getId();
            nueva.tipo = o.getTipo();
            nueva.cantidadObjetivo = o.getCantidadObjetivo();
            return nueva;
        });
        e.estatus = o.getEstatus();
        e.cantidadReal = o.getCantidadReal();
        jpa.save(e);
        return o;
    }

    @Override
    public Optional<OrdenOperacion> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> OrdenOperacion.reconstruir(e.id, e.tipo, e.estatus,
                e.cantidadObjetivo, e.cantidadReal));
    }
}
