package com.mx.josesaid.develop.clientes.application;

import java.util.List;

/** Existen coincidencias parciales (correo o nombre) y la alta no fue confirmada con confirmarDuplicado=true. */
public class DuplicadoPosibleException extends RuntimeException {

    private final List<Coincidencia> coincidencias;

    public DuplicadoPosibleException(List<Coincidencia> coincidencias) {
        super("Se encontraron registros posiblemente duplicados");
        this.coincidencias = List.copyOf(coincidencias);
    }

    public List<Coincidencia> getCoincidencias() {
        return coincidencias;
    }
}
