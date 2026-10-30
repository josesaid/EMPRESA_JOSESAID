package com.mx.josesaid.develop.pedidos.domain;

/** El recurso no existe. HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
