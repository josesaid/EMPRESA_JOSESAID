package com.mx.josesaid.develop.clientes.application;

import java.util.UUID;

/** Ya existe un cliente no dado de baja con el mismo RFC. */
public class ConflictoRfcException extends RuntimeException {

    private final UUID clienteExistenteId;

    /** Se lanza cuando el RFC ya pertenece a un cliente activo. Guarda el id de ese cliente. */
    public ConflictoRfcException(UUID clienteExistenteId) {
        super("Ya existe un cliente activo con ese RFC");
        this.clienteExistenteId = clienteExistenteId;
    }

    /** Devuelve el id del cliente que ya tiene ese RFC. */
    public UUID getClienteExistenteId() {
        return clienteExistenteId;
    }
}
