package com.mx.josesaid.develop.nexus.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Tope de tokens por usuario, diario y mensual. Se revisa antes de llamar al modelo:
 * si el usuario ya alcanzó un tope, Nexus no gasta tokens en esa consulta.
 * Los días y meses se cuentan en la hora de Ciudad de México.
 */
public final class PresupuestoTokens {

    static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private final ConsumoTokens consumo;
    private final long tokensMensuales;
    private final long tokensDiarios;
    private final Clock reloj;

    public PresupuestoTokens(ConsumoTokens consumo, long tokensMensuales, long tokensDiarios, Clock reloj) {
        this.consumo = consumo;
        this.tokensMensuales = tokensMensuales;
        this.tokensDiarios = tokensDiarios;
        this.reloj = reloj;
    }

    /** Lanza PresupuestoAgotadoException si el usuario ya alcanzó su tope diario o mensual. */
    public void exigirDisponible(String usuario) {
        LocalDate hoy = reloj.instant().atZone(ZONA).toLocalDate();
        Instant inicioDia = hoy.atStartOfDay(ZONA).toInstant();
        Instant inicioMes = hoy.withDayOfMonth(1).atStartOfDay(ZONA).toInstant();
        if (consumo.desde(usuario, inicioDia) >= tokensDiarios) {
            throw new PresupuestoAgotadoException("Alcanzaste tu límite diario de tokens. "
                    + "Se renueva mañana a las 00:00 (hora de Ciudad de México).");
        }
        if (consumo.desde(usuario, inicioMes) >= tokensMensuales) {
            throw new PresupuestoAgotadoException("Alcanzaste tu límite mensual de tokens. "
                    + "Se renueva el día 1 del próximo mes.");
        }
    }
}
