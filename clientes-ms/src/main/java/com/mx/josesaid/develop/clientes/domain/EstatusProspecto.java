package com.mx.josesaid.develop.clientes.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de un prospecto. Flujo: NUEVO -> CONTACTADO -> CALIFICADO -> CONVERTIDO.
 * PERDIDO se alcanza desde cualquier estado abierto. CONVERTIDO solo se alcanza por la operación de conversión.
 */
public enum EstatusProspecto {
    NUEVO,
    CONTACTADO,
    CALIFICADO,
    CONVERTIDO,
    PERDIDO;

    private static final Set<EstatusProspecto> ABIERTOS = EnumSet.of(NUEVO, CONTACTADO, CALIFICADO);

    public boolean esAbierto() {
        return ABIERTOS.contains(this);
    }

    /** Indica si el cambio de estatus por PATCH es válido. Repetir el estatus actual siempre es válido (idempotente). */
    public boolean puedePasarA(EstatusProspecto destino) {
        if (this == destino) {
            return true;
        }
        if (destino == CONVERTIDO) {
            return false;
        }
        if (destino == PERDIDO) {
            return esAbierto();
        }
        return switch (this) {
            case NUEVO -> destino == CONTACTADO;
            case CONTACTADO -> destino == CALIFICADO;
            case CALIFICADO, CONVERTIDO, PERDIDO -> false;
        };
    }
}
