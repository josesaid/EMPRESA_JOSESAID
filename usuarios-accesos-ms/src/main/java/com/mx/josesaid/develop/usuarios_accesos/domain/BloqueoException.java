package com.mx.josesaid.develop.usuarios_accesos.domain;

/** La cuenta está bloqueada temporalmente por intentos fallidos. Se traduce a HTTP 423. */
public class BloqueoException extends RuntimeException {
    public BloqueoException(String mensaje) {
        super(mensaje);
    }
}
