package com.mx.josesaid.develop.ui.seguridad;

/** La sesión ya no puede renovarse (expirada, revocada o inexistente): el usuario debe volver a entrar. */
public class SesionExpiradaException extends RuntimeException {

    /** Se lanza cuando la sesión ya no puede renovarse. Obliga a volver a entrar. */
    public SesionExpiradaException(String mensaje) {
        super(mensaje);
    }

    /** Igual que la anterior, con la causa original para el log. */
    public SesionExpiradaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
