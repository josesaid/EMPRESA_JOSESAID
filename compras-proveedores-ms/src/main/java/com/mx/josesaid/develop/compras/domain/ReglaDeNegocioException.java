package com.mx.josesaid.develop.compras.domain;

/** Una operación viola una regla de negocio. HTTP 422. */
public class ReglaDeNegocioException extends RuntimeException {
    /** Se lanza cuando una regla de negocio no se cumple. El mensaje se muestra al cliente. */
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
