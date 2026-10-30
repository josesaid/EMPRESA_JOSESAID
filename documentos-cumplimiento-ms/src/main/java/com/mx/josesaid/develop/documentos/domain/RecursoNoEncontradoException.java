package com.mx.josesaid.develop.documentos.domain;

/** El recurso no existe. HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
