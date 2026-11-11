package com.mx.josesaid.develop.compras.domain;

/** Conflicto con el estado actual, por ejemplo recepción mayor a lo ordenado. HTTP 409. */
public class ConflictoException extends RuntimeException {
    /** Se lanza cuando la operación choca con el estado actual. Se responde como conflicto. */
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
