package com.mx.josesaid.develop.compras.infrastructure.persistence;

import com.mx.josesaid.develop.compras.domain.Recepcion;
import com.mx.josesaid.develop.compras.domain.RecepcionRepositorio;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Component;

/** Implementa el puerto de recepciones con JPA. */
@Component
class RecepcionRepositorioJpa implements RecepcionRepositorio {

    private final RecepcionJpaRepository jpa;

    RecepcionRepositorioJpa(RecepcionJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Guarda una recepción y devuelve la guardada. */
    @Override
    public Recepcion guardar(Recepcion r) {
        RecepcionEntity e = new RecepcionEntity();
        e.id = r.id();
        e.ordenId = r.ordenId();
        e.articuloSku = r.articuloSku();
        e.cantidad = r.cantidad();
        e.fecha = LocalDate.now(ZoneId.of("America/Mexico_City"));
        jpa.save(e);
        return r;
    }
}
