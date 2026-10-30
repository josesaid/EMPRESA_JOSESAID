package com.mx.josesaid.develop.usuarios_accesos.domain;

/** Ya existe un recurso con el mismo identificador único. Se traduce a HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
