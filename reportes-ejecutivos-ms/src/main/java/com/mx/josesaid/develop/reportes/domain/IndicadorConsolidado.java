package com.mx.josesaid.develop.reportes.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Foto de un indicador dentro de un reporte. Al cerrar el reporte no cambia.
 *
 * @param id componente de tipo {@code UUID}.
 * @param area componente de tipo {@code Area}.
 * @param nombre componente de tipo {@code String}.
 * @param valor componente de tipo {@code BigDecimal}.
 * @param fuenteActualizadaUtc componente de tipo {@code Instant}.
 * @param desactualizado componente de tipo {@code boolean}.
 */
public record IndicadorConsolidado(UUID id, Area area, String nombre, BigDecimal valor,
                                   Instant fuenteActualizadaUtc, boolean desactualizado) {
}
