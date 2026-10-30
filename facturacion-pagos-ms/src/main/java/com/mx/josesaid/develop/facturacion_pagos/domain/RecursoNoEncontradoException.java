package com.mx.josesaid.develop.facturacion_pagos.domain;

/** El recurso no existe. Se traduce a HTTP 404. */
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
