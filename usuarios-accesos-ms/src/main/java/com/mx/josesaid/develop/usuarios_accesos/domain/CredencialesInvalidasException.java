package com.mx.josesaid.develop.usuarios_accesos.domain;

/**
 * Correo o contraseña incorrectos, o cuenta dada de baja.
 * Se usa el mismo mensaje en todos los casos para no revelar qué usuarios existen. Se traduce a HTTP 401.
 */
public class CredencialesInvalidasException extends RuntimeException {
    /** Se lanza cuando el correo o la contraseña no son válidos. Usa un mensaje genérico para no revelar cuál de los dos falló. */
    public CredencialesInvalidasException() {
        super("Correo o contraseña incorrectos");
    }
}
