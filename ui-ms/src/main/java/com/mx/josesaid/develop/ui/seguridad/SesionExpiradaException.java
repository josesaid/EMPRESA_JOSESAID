package com.mx.josesaid.develop.ui.seguridad;

/** La sesión ya no puede renovarse (expirada, revocada o inexistente): el usuario debe volver a entrar. */
public class SesionExpiradaException extends RuntimeException {

    public SesionExpiradaException(String mensaje) {
        super(mensaje);
    }

    public SesionExpiradaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
