package com.mx.josesaid.develop.reportes.domain;

/** Una operación viola una regla de negocio. HTTP 422. */
public class ReglaDeNegocioException extends RuntimeException {
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
