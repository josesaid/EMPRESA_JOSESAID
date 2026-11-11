package com.mx.josesaid.develop.documentos.domain;

/** Conflicto con el estado actual del documento. HTTP 409. */
public class ConflictoException extends RuntimeException {
    /** Crea la excepción con el motivo del conflicto. */
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
