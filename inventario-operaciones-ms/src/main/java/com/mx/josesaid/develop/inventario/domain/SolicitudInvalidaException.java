package com.mx.josesaid.develop.inventario.domain;

/** La solicitud no cumple requisitos de forma, como lote obligatorio ausente. HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
