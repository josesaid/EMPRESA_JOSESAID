package com.mx.josesaid.develop.pedidos.domain;

/** La cotización venció y no puede aceptarse. HTTP 410. */
public class CotizacionVencidaException extends RuntimeException {
    public CotizacionVencidaException(String mensaje) {
        super(mensaje);
    }
}
