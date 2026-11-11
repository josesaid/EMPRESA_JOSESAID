package com.mx.josesaid.develop.reportes.domain;

/** Una operación viola una regla de negocio. HTTP 422. */
public class ReglaDeNegocioException extends RuntimeException {
    /** Crea la excepción con el mensaje de la regla incumplida. */
    public ReglaDeNegocioException(String mensaje) {
        super(mensaje);
    }
}
