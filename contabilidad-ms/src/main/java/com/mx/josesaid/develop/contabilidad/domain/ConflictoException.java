package com.mx.josesaid.develop.contabilidad.domain;

/** Conflicto con el estado actual. HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
