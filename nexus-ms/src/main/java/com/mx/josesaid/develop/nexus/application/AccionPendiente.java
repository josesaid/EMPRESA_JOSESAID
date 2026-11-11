package com.mx.josesaid.develop.nexus.application;

import java.time.Instant;
import java.util.UUID;

/**
 * Acción de escritura que Nexus preparó y que espera la confirmación del usuario.
 *
 * @param id componente de tipo {@code UUID}.
 * @param usuario componente de tipo {@code String}.
 * @param tipo componente de tipo {@code String}.
 * @param payload componente de tipo {@code String}.
 * @param resumen componente de tipo {@code String}.
 * @param estatus componente de tipo {@code String}.
 * @param creadaUtc componente de tipo {@code Instant}.
 * @param expiraUtc componente de tipo {@code Instant}.
 */
public record AccionPendiente(UUID id, String usuario, String tipo, String payload, String resumen,
                              String estatus, Instant creadaUtc, Instant expiraUtc) {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String CONFIRMADA = "CONFIRMADA";
    public static final String EXPIRADA = "EXPIRADA";
    public static final String CANCELADA = "CANCELADA";

    /** Devuelve una copia de la acción con otro estatus. Los registros son inmutables; cambiar el estatus siempre crea una nueva instancia. */
    public AccionPendiente conEstatus(String nuevoEstatus) {
        return new AccionPendiente(id, usuario, tipo, payload, resumen, nuevoEstatus, creadaUtc, expiraUtc);
    }
}
