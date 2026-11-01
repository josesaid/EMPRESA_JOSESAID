package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class PresupuestoTokensTest {

    // 15 de noviembre de 2026, 12:00 en Ciudad de México (UTC-06:00).
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC);
    private static final Instant INICIO_DIA = Instant.parse("2026-11-15T06:00:00Z");
    private static final long SERVICIO = 1_000_000;

    private static ConsumoTokens consumo(ToLongBiFunction<String, Instant> porUsuario, ToLongFunction<Instant> servicio) {
        return new ConsumoTokens() {
            @Override
            public long desde(String usuario, Instant desde) {
                return porUsuario.applyAsLong(usuario, desde);
            }

            @Override
            public long delServicio(Instant desde) {
                return servicio.applyAsLong(desde);
            }
        };
    }

    private static PresupuestoTokens presupuesto(ConsumoTokens consumo, SimpleMeterRegistry registro) {
        return new PresupuestoTokens(consumo, 150_000, 15_000, SERVICIO, RELOJ, registro);
    }

    @Test
    void sinConsumoHayPresupuesto() {
        presupuesto(consumo((u, d) -> 0, d -> 0), new SimpleMeterRegistry()).exigirDisponible("u-ventas");
    }

    @Test
    void alcanzarElTopeDiarioDelUsuarioBloquea() {
        PresupuestoTokens presupuesto = presupuesto(consumo((u, d) -> 15_000, d -> 0), new SimpleMeterRegistry());

        assertThatThrownBy(() -> presupuesto.exigirDisponible("u-ventas"))
                .isInstanceOf(PresupuestoAgotadoException.class)
                .hasMessageContaining("límite diario");
    }

    @Test
    void alcanzarElTopeMensualDelUsuarioBloqueaAunqueElDiaEstePermitido() {
        PresupuestoTokens presupuesto = presupuesto(
                consumo((u, d) -> d.equals(INICIO_DIA) ? 0 : 150_000, d -> 0), new SimpleMeterRegistry());

        assertThatThrownBy(() -> presupuesto.exigirDisponible("u-ventas"))
                .isInstanceOf(PresupuestoAgotadoException.class)
                .hasMessageContaining("límite mensual");
    }

    @Test
    void alcanzarElTopeDelServicioBloqueaATodos() {
        SimpleMeterRegistry registro = new SimpleMeterRegistry();
        PresupuestoTokens presupuesto = presupuesto(consumo((u, d) -> 0, d -> SERVICIO), registro);

        assertThatThrownBy(() -> presupuesto.exigirDisponible("u-otro"))
                .isInstanceOf(PresupuestoAgotadoException.class)
                .hasMessageContaining("presupuesto mensual del servicio");
        assertThat(registro.counter("nexus.presupuesto.bloqueos", "ambito", "servicio").count()).isEqualTo(1.0);
    }

    @Test
    void cruzarElOchentaPorCientoAvisaUnaVezPeroNoBloquea() {
        SimpleMeterRegistry registro = new SimpleMeterRegistry();
        PresupuestoTokens presupuesto = presupuesto(consumo((u, d) -> 0, d -> 850_000), registro);

        presupuesto.exigirDisponible("u-ventas");
        presupuesto.exigirDisponible("u-ventas");

        assertThat(registro.counter("nexus.presupuesto.umbral.alcanzado").count()).isEqualTo(1.0);
        assertThat(registro.counter("nexus.presupuesto.bloqueos", "ambito", "servicio").count()).isEqualTo(0.0);
    }

    @Test
    void elGaugeMuestraElConsumoMensualDelServicio() {
        SimpleMeterRegistry registro = new SimpleMeterRegistry();
        presupuesto(consumo((u, d) -> 0, d -> 123_456), registro).exigirDisponible("u-ventas");

        assertThat(registro.get("nexus.presupuesto.servicio.consumo").gauge().value()).isEqualTo(123_456.0);
    }
}
