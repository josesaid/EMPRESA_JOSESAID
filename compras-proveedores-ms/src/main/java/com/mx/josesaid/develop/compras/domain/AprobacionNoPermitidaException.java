package com.mx.josesaid.develop.compras.domain;

/** El usuario no puede aprobar esta orden por segregación de funciones o por su monto. HTTP 403. */
public class AprobacionNoPermitidaException extends RuntimeException {
    public AprobacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
