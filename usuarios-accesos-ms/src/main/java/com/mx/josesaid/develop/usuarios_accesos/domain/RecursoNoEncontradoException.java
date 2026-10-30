package com.mx.josesaid.develop.usuarios_accesos.domain;

/** El recurso solicitado no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
