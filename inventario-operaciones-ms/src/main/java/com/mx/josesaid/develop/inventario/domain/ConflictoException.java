package com.mx.josesaid.develop.inventario.domain;

/** Conflicto con el estado actual, por ejemplo existencia insuficiente. HTTP 409. */
public class ConflictoException extends RuntimeException {
    /** Se lanza cuando la operación choca con el estado actual, por ejemplo por falta de existencia. Se responde como conflicto. */
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
