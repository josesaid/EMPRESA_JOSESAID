package com.mx.josesaid.develop.compras.domain;

/** Solicitud con datos de forma inválidos, como motivo vacío. HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    /** Se lanza cuando la solicitud no es válida. */
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
