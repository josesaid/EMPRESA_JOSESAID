package com.mx.josesaid.develop.compras.domain;

/** El usuario no puede aprobar esta orden por segregación de funciones o por su monto. HTTP 403. */
public class AprobacionNoPermitidaException extends RuntimeException {
    /** Se lanza cuando el usuario no puede aprobar la orden: por ser quien la creó, por el límite de monto o por ser una orden sin creador registrado. */
    public AprobacionNoPermitidaException(String mensaje) {
        super(mensaje);
    }
}
