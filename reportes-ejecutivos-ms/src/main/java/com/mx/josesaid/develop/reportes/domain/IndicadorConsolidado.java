package com.mx.josesaid.develop.reportes.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Foto de un indicador dentro de un reporte. Al cerrar el reporte no cambia.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param area área de negocio a la que pertenece, de tipo {@code Area}.
 * @param nombre nombre descriptivo, de tipo {@code String}.
 * @param valor valor registrado, de tipo {@code BigDecimal}.
 * @param fuenteActualizadaUtc fecha y hora (UTC) de la última actualización de la fuente, de tipo {@code Instant}.
 * @param desactualizado desactualizado, de tipo {@code boolean}.
 */
public record IndicadorConsolidado(UUID id, Area area, String nombre, BigDecimal valor,
                                   Instant fuenteActualizadaUtc, boolean desactualizado) {
}
