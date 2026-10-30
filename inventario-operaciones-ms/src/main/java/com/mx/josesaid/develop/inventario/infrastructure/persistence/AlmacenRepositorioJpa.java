package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import com.mx.josesaid.develop.inventario.domain.Almacen;
import com.mx.josesaid.develop.inventario.domain.AlmacenRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class AlmacenRepositorioJpa implements AlmacenRepositorio {

    private final AlmacenJpaRepository jpa;

    AlmacenRepositorioJpa(AlmacenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Almacen guardar(Almacen a) {
        AlmacenEntity e = new AlmacenEntity();
        e.id = a.getId();
        e.nombre = a.getNombre();
        e.ubicacion = a.getUbicacion();
        jpa.save(e);
        return a;
    }

    @Override
    public Optional<Almacen> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> Almacen.reconstruir(e.id, e.nombre, e.ubicacion));
    }
}
