package com.mx.josesaid.develop.nexus.application;

/** La acción ya fue confirmada, cancelada o expiró. */
public class AccionNoPendienteException extends RuntimeException {

    public AccionNoPendienteException(String mensaje) {
        super(mensaje);
    }
}
