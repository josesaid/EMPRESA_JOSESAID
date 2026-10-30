package com.mx.josesaid.develop.usuarios_accesos.domain;

/**
 * La cuenta tiene una contraseña temporal pendiente de cambio. Se traduce a HTTP 403.
 */
public class CambioPasswordRequeridoException extends RuntimeException {
    public CambioPasswordRequeridoException() {
        super("Debe cambiar su contraseña temporal antes de iniciar sesión");
    }
}
