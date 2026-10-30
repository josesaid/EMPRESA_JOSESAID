package com.mx.josesaid.develop.clientes.application;

import java.util.UUID;

/** Ya existe un cliente no dado de baja con el mismo RFC. */
public class ConflictoRfcException extends RuntimeException {

    private final UUID clienteExistenteId;

    public ConflictoRfcException(UUID clienteExistenteId) {
        super("Ya existe un cliente activo con ese RFC");
        this.clienteExistenteId = clienteExistenteId;
    }

    public UUID getClienteExistenteId() {
        return clienteExistenteId;
    }
}
