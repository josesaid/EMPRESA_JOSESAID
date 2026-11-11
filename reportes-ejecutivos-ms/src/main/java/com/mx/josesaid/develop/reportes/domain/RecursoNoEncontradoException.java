package com.mx.josesaid.develop.reportes.domain;

/** El recurso no existe. HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    /** Crea la excepción con el mensaje que se mostrará al cliente. */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
