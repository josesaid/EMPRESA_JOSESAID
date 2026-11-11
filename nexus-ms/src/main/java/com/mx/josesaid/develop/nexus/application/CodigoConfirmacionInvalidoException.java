package com.mx.josesaid.develop.nexus.application;

/** El código no coincide. La acción se cancela para que no pueda adivinarse por intentos repetidos. */
public class CodigoConfirmacionInvalidoException extends RuntimeException {

    /** Crea la excepción con el mensaje que se mostrará al usuario. */
    public CodigoConfirmacionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
