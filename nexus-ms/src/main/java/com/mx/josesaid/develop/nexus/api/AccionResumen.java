package com.mx.josesaid.develop.nexus.api;

import java.time.Instant;
import java.util.UUID;

/** Lo que el usuario ve de una acción pendiente. El payload técnico no sale de la API. */
public record AccionResumen(UUID id, String tipo, String resumen, String estatus, Instant expiraUtc) {
}
