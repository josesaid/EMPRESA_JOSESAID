package com.mx.josesaid.develop.pedidos.domain;

/** El recurso no existe. HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    /** Se lanza cuando no existe el recurso pedido. */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
