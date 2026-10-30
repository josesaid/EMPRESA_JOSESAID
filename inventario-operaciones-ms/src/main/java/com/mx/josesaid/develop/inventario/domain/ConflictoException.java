package com.mx.josesaid.develop.inventario.domain;

/** Conflicto con el estado actual, por ejemplo existencia insuficiente. HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
