package com.mx.josesaid.develop.ui.nexus;

import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de nexus-ms al proponer una póliza. El código de confirmación solo llega una vez.
 *
 * @param id componente de tipo {@code UUID}.
 * @param tipo componente de tipo {@code String}.
 * @param resumen componente de tipo {@code String}.
 * @param estatus componente de tipo {@code String}.
 * @param expiraUtc componente de tipo {@code Instant}.
 * @param codigoConfirmacion componente de tipo {@code String}.
 */
public record SugerenciaDto(UUID id, String tipo, String resumen, String estatus, Instant expiraUtc,
                            String codigoConfirmacion) {
}
