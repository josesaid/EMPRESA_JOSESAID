package com.mx.josesaid.develop.nexus.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que el usuario ve de una acción pendiente. El payload técnico no sale de la API.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param tipo tipo o clasificación del registro, de tipo {@code String}.
 * @param resumen resumen breve, de tipo {@code String}.
 * @param estatus estado actual del registro, de tipo {@code String}.
 * @param expiraUtc expira utc, de tipo {@code Instant}.
 */
public record AccionResumen(UUID id, String tipo, String resumen, String estatus, Instant expiraUtc) {
}
