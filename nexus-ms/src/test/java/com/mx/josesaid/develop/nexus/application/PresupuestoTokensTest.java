package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class PresupuestoTokensTest {

    // 15 de noviembre de 2026, 12:00 en Ciudad de México (UTC-06:00).
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC);
    private static final Instant INICIO_DIA = Instant.parse("2026-11-15T06:00:00Z");

    @Test
    void sinConsumoHayPresupuesto() {
        PresupuestoTokens presupuesto = new PresupuestoTokens((u, d) -> 0, 150_000, 15_000, RELOJ);

        presupuesto.exigirDisponible("u-ventas");
    }

    @Test
    void alcanzarElTopeDiarioBloquea() {
        PresupuestoTokens presupuesto = new PresupuestoTokens((u, d) -> 15_000, 150_000, 15_000, RELOJ);

        assertThatThrownBy(() -> presupuesto.exigirDisponible("u-ventas"))
                .isInstanceOf(PresupuestoAgotadoException.class)
                .hasMessageContaining("límite diario");
    }

    @Test
    void alcanzarElTopeMensualBloqueaAunqueElDiaEstePermitido() {
        // Ningún consumo desde el inicio del día; mucho desde el inicio del mes.
        PresupuestoTokens presupuesto = new PresupuestoTokens(
                (u, d) -> d.equals(INICIO_DIA) ? 0 : 150_000, 150_000, 15_000, RELOJ);

        assertThatThrownBy(() -> presupuesto.exigirDisponible("u-ventas"))
                .isInstanceOf(PresupuestoAgotadoException.class)
                .hasMessageContaining("límite mensual");
    }

    @Test
    void elConsumoSeMideDelUsuarioQueConsulta() {
        PresupuestoTokens presupuesto = new PresupuestoTokens(
                (u, d) -> "u-ventas".equals(u) ? 20_000 : 0, 150_000, 15_000, RELOJ);

        presupuesto.exigirDisponible("u-otro");
        assertThatThrownBy(() -> presupuesto.exigirDisponible("u-ventas"))
                .isInstanceOf(PresupuestoAgotadoException.class);
    }
}
