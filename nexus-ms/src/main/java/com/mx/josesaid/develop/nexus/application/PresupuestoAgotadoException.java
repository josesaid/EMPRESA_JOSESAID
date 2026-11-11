package com.mx.josesaid.develop.nexus.application;

/** Se lanza cuando un presupuesto de tokens está agotado. Responde 429 en la API. */
public class PresupuestoAgotadoException extends RuntimeException {

    /** Crea la excepción con el mensaje que se mostrará al usuario. */
    public PresupuestoAgotadoException(String mensaje) {
        super(mensaje);
    }
}
