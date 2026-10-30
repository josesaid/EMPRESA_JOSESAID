package com.mx.josesaid.develop.facturacion_pagos.domain;

/** Una operación viola una regla de negocio. Se traduce a HTTP 422. */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
