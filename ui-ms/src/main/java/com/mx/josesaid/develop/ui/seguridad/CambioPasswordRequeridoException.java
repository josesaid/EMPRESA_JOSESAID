package com.mx.josesaid.develop.ui.seguridad;

import org.springframework.security.core.AuthenticationException;

/** La cuenta tiene una contraseña temporal y debe cambiarla antes de iniciar sesión. */
public class CambioPasswordRequeridoException extends AuthenticationException {

    /** Se lanza cuando la contraseña es temporal y debe cambiarse antes de entrar. */
    public CambioPasswordRequeridoException(String mensaje) {
        super(mensaje);
    }
}
