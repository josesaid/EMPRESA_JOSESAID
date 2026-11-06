package com.mx.josesaid.develop.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class LimitadorVentanaTest {

    private static final Instant INICIO = Instant.parse("2026-11-05T12:00:00Z");

    private static Clock relojMovible(AtomicReference<Instant> instante) {
        return new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zone) {
                return this;
            }

            @Override
            public Instant instant() {
                return instante.get();
            }
        };
    }

    @Test
    void permiteHastaElMaximoYBloqueaElSiguiente() {
        var instante = new AtomicReference<>(INICIO);
        var limitador = new LimitadorVentana(3, Duration.ofMinutes(1), relojMovible(instante));

        assertThat(limitador.intentar("ip").permitido()).isTrue();
        assertThat(limitador.intentar("ip").permitido()).isTrue();
        assertThat(limitador.intentar("ip").permitido()).isTrue();

        var bloqueado = limitador.intentar("ip");
        assertThat(bloqueado.permitido()).isFalse();
        assertThat(bloqueado.reintentarEnSegundos()).isEqualTo(60);
    }

    @Test
    void cadaClaveTieneSuPropioConteo() {
        var instante = new AtomicReference<>(INICIO);
        var limitador = new LimitadorVentana(1, Duration.ofMinutes(1), relojMovible(instante));

        assertThat(limitador.intentar("ip-a").permitido()).isTrue();
        assertThat(limitador.intentar("ip-a").permitido()).isFalse();
        assertThat(limitador.intentar("ip-b").permitido()).isTrue();
    }

    @Test
    void reiniciaCuandoPasaLaVentana() {
        var instante = new AtomicReference<>(INICIO);
        var limitador = new LimitadorVentana(1, Duration.ofMinutes(1), relojMovible(instante));

        limitador.intentar("ip");
        assertThat(limitador.intentar("ip").permitido()).isFalse();

        instante.set(INICIO.plusSeconds(61));
        assertThat(limitador.intentar("ip").permitido()).isTrue();
    }

    @Test
    void reintentarEnSegundosBajaConElTiempo() {
        var instante = new AtomicReference<>(INICIO);
        var limitador = new LimitadorVentana(1, Duration.ofMinutes(1), relojMovible(instante));

        limitador.intentar("ip");
        instante.set(INICIO.plusSeconds(20));

        assertThat(limitador.intentar("ip").reintentarEnSegundos()).isEqualTo(40);
    }

    @Test
    void rechazaMaximoMenorQueUno() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new LimitadorVentana(0, Duration.ofMinutes(1), Clock.systemUTC()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
