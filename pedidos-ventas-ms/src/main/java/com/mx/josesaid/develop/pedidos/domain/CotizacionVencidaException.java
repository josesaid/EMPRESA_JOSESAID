package com.mx.josesaid.develop.pedidos.domain;

/** La cotización venció y no puede aceptarse. HTTP 410. */
public class CotizacionVencidaException extends RuntimeException {
    /** Se lanza cuando la cotización ya venció. Responde 410. */
    public CotizacionVencidaException(String mensaje) {
        super(mensaje);
    }
}
