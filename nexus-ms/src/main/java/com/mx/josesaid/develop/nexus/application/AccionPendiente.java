package com.mx.josesaid.develop.nexus.application;

import java.time.Instant;
import java.util.UUID;

/** Acción de escritura que Nexus preparó y que espera la confirmación del usuario. */
public record AccionPendiente(UUID id, String usuario, String tipo, String payload, String resumen,
                              String estatus, Instant creadaUtc, Instant expiraUtc) {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String EXPIRADA = "EXPIRADA";

    public AccionPendiente conEstatus(String nuevoEstatus) {
        return new AccionPendiente(id, usuario, tipo, payload, resumen, nuevoEstatus, creadaUtc, expiraUtc);
    }
}
