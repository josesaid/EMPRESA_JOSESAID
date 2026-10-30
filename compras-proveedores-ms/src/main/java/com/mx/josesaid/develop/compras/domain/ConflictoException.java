package com.mx.josesaid.develop.compras.domain;

/** Conflicto con el estado actual, por ejemplo recepción mayor a lo ordenado. HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
