package com.mx.josesaid.develop.ui.nexus;

import java.time.Instant;
import java.util.UUID;

/** Respuesta de nexus-ms al proponer una póliza. El código de confirmación solo llega una vez. */
public record SugerenciaDto(UUID id, String tipo, String resumen, String estatus, Instant expiraUtc,
                            String codigoConfirmacion) {
}
