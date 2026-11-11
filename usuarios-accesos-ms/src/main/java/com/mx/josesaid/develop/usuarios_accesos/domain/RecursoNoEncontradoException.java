package com.mx.josesaid.develop.usuarios_accesos.domain;

/** El recurso solicitado no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    /** Se lanza cuando no existe el recurso pedido. */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
