package com.mx.josesaid.develop.reportes.domain;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/** Valor de un indicador de un área en un periodo. Se identifica por área, nombre y periodo. */
public record Indicador(UUID id, Area area, String nombre, BigDecimal valor, Periodo periodo,
                        Instant fuenteActualizadaUtc) {

    public Indicador {
        if (area == null) {
            throw new SolicitudInvalidaException("El área es obligatoria");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new SolicitudInvalidaException("El nombre del indicador es obligatorio");
        }
        if (valor == null) {
            throw new SolicitudInvalidaException("El valor es obligatorio");
        }
        if (periodo == null) {
            throw new SolicitudInvalidaException("El periodo es obligatorio");
        }
        if (fuenteActualizadaUtc == null) {
            throw new SolicitudInvalidaException("La fecha de actualización de la fuente es obligatoria");
        }
        nombre = nombre.trim();
    }

    public static Indicador nuevo(Area area, String nombre, Periodo periodo, BigDecimal valor, Instant fuente) {
        return new Indicador(UUID.randomUUID(), area, nombre, valor, periodo, fuente);
    }

    /** Un indicador cuya fuente no se actualizó dentro del umbral se marca como desactualizado. */
    public boolean desactualizado(Instant ahora, Duration umbral) {
        return fuenteActualizadaUtc.isBefore(ahora.minus(umbral));
    }
}
