package com.mx.josesaid.develop.documentos.domain;

/** El recurso no existe. HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    /** Crea la excepción con el mensaje que se mostrará al cliente. */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
