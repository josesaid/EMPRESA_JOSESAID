package com.mx.josesaid.develop.nexus.api;

import java.time.Instant;
import java.util.UUID;

/**
 * Lo que el usuario ve de una acción pendiente. El payload técnico no sale de la API.
 *
 * @param id componente de tipo {@code UUID}.
 * @param tipo componente de tipo {@code String}.
 * @param resumen componente de tipo {@code String}.
 * @param estatus componente de tipo {@code String}.
 * @param expiraUtc componente de tipo {@code Instant}.
 */
public record AccionResumen(UUID id, String tipo, String resumen, String estatus, Instant expiraUtc) {
}
