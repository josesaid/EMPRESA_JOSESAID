package com.mx.josesaid.develop.facturacion_pagos.domain;

/** Conflicto con el estado actual del recurso. Se traduce a HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
