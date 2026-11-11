package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AlmacenAcciones;

/** Guarda las acciones pendientes en PostgreSQL con JPA. */
@Component
public class AlmacenAccionesJpa implements AlmacenAcciones {

    private final AccionPendienteRepositorio repositorio;

    /** Recibe el repositorio de acciones pendientes. */
    public AlmacenAccionesJpa(AccionPendienteRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    /** Guarda o actualiza una acción pendiente. */
    @Override
    public void guardar(AccionPendiente a) {
        repositorio.save(new AccionPendienteEntidad(a.id(), a.usuario(), a.tipo(), a.payload(), a.resumen(),
                a.estatus(), a.creadaUtc(), a.expiraUtc()));
    }

    /** Busca una acción pendiente por su identificador. */
    @Override
    public Optional<AccionPendiente> buscar(UUID id) {
        return repositorio.findById(id).map(e -> new AccionPendiente(e.getId(), e.getUsuario(), e.getTipo(),
                e.getPayload(), e.getResumen(), e.getEstatus(), e.getCreadaUtc(), e.getExpiraUtc()));
    }
}
