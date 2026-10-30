package com.mx.josesaid.develop.nexus.comun.tokens;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class RegistroTokensTest {

    @Test
    void sumaTokensPorServicioYUsuario() {
        var registro = new SimpleMeterRegistry();
        var tokens = new RegistroTokens(registro, PreciosTokens.SIN_PRECIO);

        tokens.registrar("reportes", "u1", 100, 50);
        tokens.registrar("reportes", "u1", 20, 5);

        assertThat(registro.get(RegistroTokens.METRICA_TOKENS)
                .tags("servicio", "reportes", "usuario", "u1", "direccion", "entrada")
                .counter().count()).isEqualTo(120.0);
        assertThat(registro.get(RegistroTokens.METRICA_TOKENS)
                .tags("servicio", "reportes", "usuario", "u1", "direccion", "salida")
                .counter().count()).isEqualTo(55.0);
    }

    @Test
    void calculaCostoEstimadoPorMillonDeTokens() {
        var registro = new SimpleMeterRegistry();
        var precios = new PreciosTokens(new BigDecimal("3"), new BigDecimal("15"));
        var tokens = new RegistroTokens(registro, precios);

        // 1M de entrada a 3 + 500k de salida a 15 = 3 + 7.5 = 10.5
        tokens.registrar("clientes", "u2", 1_000_000, 500_000);

        assertThat(registro.get(RegistroTokens.METRICA_COSTO)
                .tags("servicio", "clientes", "usuario", "u2")
                .counter().count()).isEqualTo(10.5);
    }

    @Test
    void sinPreciosNoRegistraCosto() {
        var registro = new SimpleMeterRegistry();
        new RegistroTokens(registro, PreciosTokens.SIN_PRECIO).registrar("x", "u", 10, 10);

        assertThat(registro.find(RegistroTokens.METRICA_COSTO).counter()).isNull();
    }

    @Test
    void usaDesconocidoSiFaltanServicioOUsuario() {
        var registro = new SimpleMeterRegistry();
        new RegistroTokens(registro, null).registrar(null, " ", 5, 0);

        assertThat(registro.get(RegistroTokens.METRICA_TOKENS)
                .tags("servicio", "desconocido", "usuario", "desconocido", "direccion", "entrada")
                .counter().count()).isEqualTo(5.0);
    }

    @Test
    void rechazaPreciosNegativos() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> new PreciosTokens(new BigDecimal("-1"), BigDecimal.ONE))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
