package com.mx.josesaid.develop.ui.nexus;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de nexus-ms al proponer una póliza. El código de confirmación solo llega una vez.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param tipo tipo o clasificación del registro, de tipo {@code String}.
 * @param resumen resumen breve, de tipo {@code String}.
 * @param estatus estado actual del registro, de tipo {@code String}.
 * @param expiraUtc expira utc, de tipo {@code Instant}.
 * @param codigoConfirmacion codigo confirmacion, de tipo {@code String}.
 */
public record SugerenciaDto(UUID id, String tipo, String resumen, String estatus, Instant expiraUtc,
                            String codigoConfirmacion) {
}
