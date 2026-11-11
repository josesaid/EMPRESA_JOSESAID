package com.mx.josesaid.develop.clientes.application;

import java.util.UUID;

/** Se lanza cuando no existe el recurso pedido. */
public class EntidadNoEncontradaException extends RuntimeException {

    /** Crea la excepción indicando el tipo de entidad y su id. */
    public EntidadNoEncontradaException(String tipo, UUID id) {
        super("No existe " + tipo + " con id " + id);
    }
}
