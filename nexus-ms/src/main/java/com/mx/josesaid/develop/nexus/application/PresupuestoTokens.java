package com.mx.josesaid.develop.nexus.application;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Topes de tokens: por usuario (diario y mensual) y por servicio (mensual).
 * Se revisan antes de llamar al modelo: si un tope se alcanzó, Nexus no gasta tokens en esa consulta.
 * Los días y meses se cuentan en la hora de Ciudad de México.
 *
 * Observabilidad: métricas Prometheus (bloqueos por ámbito, consumo mensual del servicio y su límite),
 * un aviso en log al cruzar el 80% del tope de servicio y un error en log cada vez que se bloquea.
 */
public final class PresupuestoTokens {

    static final ZoneId ZONA = ZoneId.of("America/Mexico_City");
    static final double UMBRAL_AVISO = 0.8;

    private static final Logger log = LoggerFactory.getLogger(PresupuestoTokens.class);

    private final ConsumoTokens consumo;
    private final long tokensMensuales;
    private final long tokensDiarios;
    private final long tokensServicioMensuales;
    private final Clock reloj;
    private final MeterRegistry registro;
    private final AtomicLong consumoServicioMes = new AtomicLong();
    private final AtomicReference<YearMonth> avisoEmitido = new AtomicReference<>();

    public PresupuestoTokens(ConsumoTokens consumo, long tokensMensuales, long tokensDiarios,
                             long tokensServicioMensuales, Clock reloj, MeterRegistry registro) {
        this.consumo = consumo;
        this.tokensMensuales = tokensMensuales;
        this.tokensDiarios = tokensDiarios;
        this.tokensServicioMensuales = tokensServicioMensuales;
        this.reloj = reloj;
        this.registro = registro;
        Gauge.builder("nexus.presupuesto.servicio.consumo", consumoServicioMes, AtomicLong::get)
                .description("Tokens consumidos por nexus-ms en el mes en curso")
                .register(registro);
        Gauge.builder("nexus.presupuesto.servicio.limite", () -> tokensServicioMensuales)
                .description("Tope mensual de tokens de nexus-ms")
                .register(registro);
    }

    /** Lanza PresupuestoAgotadoException si el usuario o el servicio ya alcanzaron su tope. */
    public void exigirDisponible(String usuario) {
        LocalDate hoy = reloj.instant().atZone(ZONA).toLocalDate();
        Instant inicioDia = hoy.atStartOfDay(ZONA).toInstant();
        Instant inicioMes = hoy.withDayOfMonth(1).atStartOfDay(ZONA).toInstant();

        if (consumo.desde(usuario, inicioDia) >= tokensDiarios) {
            bloquear("usuario-diario", usuario, "Alcanzaste tu límite diario de tokens. "
                    + "Se renueva mañana a las 00:00 (hora de Ciudad de México).");
        }
        if (consumo.desde(usuario, inicioMes) >= tokensMensuales) {
            bloquear("usuario-mensual", usuario, "Alcanzaste tu límite mensual de tokens. "
                    + "Se renueva el día 1 del próximo mes.");
        }
        revisarServicio(hoy, inicioMes, usuario);
    }

    private void revisarServicio(LocalDate hoy, Instant inicioMes, String usuario) {
        long usado = consumo.delServicio(inicioMes);
        consumoServicioMes.set(usado);
        if (usado >= tokensServicioMensuales) {
            bloquear("servicio", usuario, "Nexus alcanzó el presupuesto mensual del servicio. "
                    + "Se renueva el día 1 del próximo mes; avisa al administrador si necesitas usarlo antes.");
        }
        if (usado >= tokensServicioMensuales * UMBRAL_AVISO) {
            avisarUmbral(YearMonth.from(hoy), usado);
        }
    }

    private void avisarUmbral(YearMonth mes, long usado) {
        YearMonth previo = avisoEmitido.get();
        if (mes.equals(previo) || !avisoEmitido.compareAndSet(previo, mes)) {
            return;
        }
        registro.counter("nexus.presupuesto.umbral.alcanzado").increment();
        log.warn("Nexus superó el {}% de su presupuesto mensual de tokens: {} de {} en {}",
                (int) (UMBRAL_AVISO * 100), usado, tokensServicioMensuales, mes);
    }

    private void bloquear(String ambito, String usuario, String mensaje) {
        registro.counter("nexus.presupuesto.bloqueos", "ambito", ambito).increment();
        log.warn("Consulta bloqueada por presupuesto de tokens: ámbito={} usuario={}", ambito, usuario);
        throw new PresupuestoAgotadoException(mensaje);
    }
}
