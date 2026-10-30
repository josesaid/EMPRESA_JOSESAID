package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import com.mx.josesaid.develop.inventario.domain.Existencia;
import com.mx.josesaid.develop.inventario.domain.ExistenciaRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class ExistenciaRepositorioJpa implements ExistenciaRepositorio {

    private final ExistenciaJpaRepository jpa;

    ExistenciaRepositorioJpa(ExistenciaJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Actualiza la fila existente (con control de versión) o crea la del par artículo-almacén. */
    @Override
    public Existencia guardar(Existencia x) {
        ExistenciaEntity e = jpa.findByArticuloIdAndAlmacenId(x.getArticuloId(), x.getAlmacenId())
                .orElseGet(() -> {
                    ExistenciaEntity nueva = new ExistenciaEntity();
                    nueva.id = UUID.randomUUID();
                    nueva.articuloId = x.getArticuloId();
                    nueva.almacenId = x.getAlmacenId();
                    return nueva;
                });
        e.cantidad = x.getCantidad();
        e.reservado = x.getReservado();
        jpa.save(e);
        return x;
    }

    @Override
    public Optional<Existencia> buscar(UUID articuloId, UUID almacenId) {
        return jpa.findByArticuloIdAndAlmacenId(articuloId, almacenId).map(this::aDominio);
    }

    @Override
    public List<Existencia> buscarPorArticulo(UUID articuloId) {
        return jpa.findByArticuloId(articuloId).stream().map(this::aDominio).toList();
    }

    private Existencia aDominio(ExistenciaEntity e) {
        return Existencia.reconstruir(e.articuloId, e.almacenId, e.cantidad, e.reservado);
    }
}
