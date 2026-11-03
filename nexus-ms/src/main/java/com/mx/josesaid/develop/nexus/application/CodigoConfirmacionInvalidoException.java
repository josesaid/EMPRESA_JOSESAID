package com.mx.josesaid.develop.nexus.application;

/** El código no coincide. La acción se cancela para que no pueda adivinarse por intentos repetidos. */
public class CodigoConfirmacionInvalidoException extends RuntimeException {

    public CodigoConfirmacionInvalidoException(String mensaje) {
        super(mensaje);
    }
}
