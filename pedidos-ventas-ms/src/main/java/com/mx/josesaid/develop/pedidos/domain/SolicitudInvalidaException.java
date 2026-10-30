package com.mx.josesaid.develop.pedidos.domain;

/** Datos de forma inválidos, como un motivo fuera de catálogo. HTTP 400. */
public class SolicitudInvalidaException extends RuntimeException {
    public SolicitudInvalidaException(String mensaje) {
        super(mensaje);
    }
}
