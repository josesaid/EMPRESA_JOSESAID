package com.mx.josesaid.develop.pedidos.domain;

/** Conflicto con el estado actual. HTTP 409. */
public class ConflictoException extends RuntimeException {
    /** Se lanza cuando la operación choca con el estado actual del documento. Se responde como conflicto. */
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
