package com.mx.josesaid.develop.inventario.domain;

/** La solicitud no cumple requisitos de forma, como lote obligatorio ausente. HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    /** Se lanza cuando la solicitud no es válida. */
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
