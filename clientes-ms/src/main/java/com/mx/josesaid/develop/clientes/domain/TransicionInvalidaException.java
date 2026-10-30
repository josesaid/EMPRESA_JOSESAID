package com.mx.josesaid.develop.clientes.domain;

/** Se lanza cuando un cambio de estatus viola las reglas de la máquina de estados. */
public class TransicionInvalidaException extends RuntimeException {

    public TransicionInvalidaException(String mensaje) {
        super(mensaje);
    }
}
