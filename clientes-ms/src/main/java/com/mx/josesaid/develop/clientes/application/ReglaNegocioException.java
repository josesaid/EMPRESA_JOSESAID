package com.mx.josesaid.develop.clientes.application;

/** Una regla de negocio impide la operación y no es un conflicto de estado (responde 422). */
public class ReglaNegocioException extends RuntimeException {

    /** Se lanza cuando una regla de negocio no se cumple. El mensaje se muestra al usuario. */
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
