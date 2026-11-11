package com.mx.josesaid.develop.clientes.domain;

/**
 * Estados de un cliente. ACTIVO y SUSPENDIDO se alternan entre sí; cualquier estado puede pasar a BAJA.
 * BAJA es terminal: un cliente dado de baja no se reactiva.
 */
public enum EstatusCliente {
    ACTIVO,
    SUSPENDIDO,
    BAJA;

    /** Indica si el cliente puede pasar al estatus indicado. Las transiciones no permitidas se rechazan. */
    public boolean puedePasarA(EstatusCliente destino) {
        if (this == destino) {
            return true;
        }
        return this != BAJA;
    }
}
