package com.mx.josesaid.develop.nexus.application;

/** La acción no existe o no pertenece al usuario que la confirma. */
public class AccionNoEncontradaException extends RuntimeException {

    public AccionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
