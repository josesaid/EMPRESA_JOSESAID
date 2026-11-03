package com.mx.josesaid.develop.ui.seguridad;

import org.springframework.security.core.AuthenticationException;

/** La cuenta tiene una contraseña temporal y debe cambiarla antes de iniciar sesión. */
public class CambioPasswordRequeridoException extends AuthenticationException {

    public CambioPasswordRequeridoException(String mensaje) {
        super(mensaje);
    }
}
