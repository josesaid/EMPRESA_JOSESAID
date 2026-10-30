package com.mx.josesaid.develop.clientes.application;

/** Una regla de negocio impide la operación y no es un conflicto de estado (responde 422). */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
