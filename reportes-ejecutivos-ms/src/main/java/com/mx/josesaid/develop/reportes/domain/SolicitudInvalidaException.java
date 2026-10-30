package com.mx.josesaid.develop.reportes.domain;

/** Datos de forma inválidos. HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
