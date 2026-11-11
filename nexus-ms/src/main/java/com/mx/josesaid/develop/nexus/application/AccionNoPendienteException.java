package com.mx.josesaid.develop.nexus.application;

/** La acción ya fue confirmada, cancelada o expiró. */
public class AccionNoPendienteException extends RuntimeException {

    /** Crea la excepción con el mensaje que se mostrará al usuario. */
    public AccionNoPendienteException(String mensaje) {
        super(mensaje);
    }
}
