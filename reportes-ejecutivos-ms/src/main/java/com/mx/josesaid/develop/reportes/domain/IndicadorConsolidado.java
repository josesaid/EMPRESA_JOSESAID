package com.mx.josesaid.develop.reportes.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Foto de un indicador dentro de un reporte. Al cerrar el reporte no cambia. */
public record IndicadorConsolidado(UUID id, Area area, String nombre, BigDecimal valor,
                                   Instant fuenteActualizadaUtc, boolean desactualizado) {
}
