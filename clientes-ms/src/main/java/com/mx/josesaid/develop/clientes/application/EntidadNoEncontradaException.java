package com.mx.josesaid.develop.clientes.application;

import java.util.UUID;

public class EntidadNoEncontradaException extends RuntimeException {

    public EntidadNoEncontradaException(String tipo, UUID id) {
        super("No existe " + tipo + " con id " + id);
    }
}
