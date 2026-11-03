package com.mx.josesaid.develop.ui.seguridad;

import org.springframework.security.core.AuthenticationException;

/** El correo está bloqueado temporalmente por demasiados intentos fallidos. */
public class BloqueoLoginException extends AuthenticationException {

    public BloqueoLoginException(String mensaje) {
        super(mensaje);
    }
}
