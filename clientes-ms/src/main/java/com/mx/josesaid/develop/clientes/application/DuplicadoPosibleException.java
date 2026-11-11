package com.mx.josesaid.develop.clientes.application;

import java.util.List;

/** Existen coincidencias parciales (correo o nombre) y la alta no fue confirmada con confirmarDuplicado=true. */
public class DuplicadoPosibleException extends RuntimeException {

    private final List<Coincidencia> coincidencias;

    /** Se lanza cuando hay coincidencias parciales y el usuario aún no confirmó. Guarda las coincidencias para mostrarlas. */
    public DuplicadoPosibleException(List<Coincidencia> coincidencias) {
        super("Se encontraron registros posiblemente duplicados");
        this.coincidencias = List.copyOf(coincidencias);
    }

    /** Devuelve las coincidencias encontradas. */
    public List<Coincidencia> getCoincidencias() {
        return coincidencias;
    }
}
