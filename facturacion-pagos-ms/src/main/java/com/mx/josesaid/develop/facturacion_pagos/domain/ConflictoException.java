package com.mx.josesaid.develop.facturacion_pagos.domain;

/** Conflicto con el estado actual del recurso. Se traduce a HTTP 409. */
public class ConflictoException extends RuntimeException {
    /** Se lanza cuando la operación choca con el estado actual. Se responde como conflicto. */
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
