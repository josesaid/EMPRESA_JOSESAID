package com.mx.josesaid.develop.nexus.application;

/** La acción no existe o no pertenece al usuario que la confirma. */
public class AccionNoEncontradaException extends RuntimeException {

    /** Crea la excepción con el mensaje que se mostrará al usuario. */
    public AccionNoEncontradaException(String mensaje) {
        super(mensaje);
    }
}
