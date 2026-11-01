package com.mx.josesaid.develop.nexus.application;

import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de acciones pendientes. La implementación vive en infrastructure. */
public interface AlmacenAcciones {

    void guardar(AccionPendiente accion);

    Optional<AccionPendiente> buscar(UUID id);
}
