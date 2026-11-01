package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RecuperadorPoliticasTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void sinRolesNoConsultaNada() {
        AlmacenRegistrado almacen = new AlmacenRegistrado();
        RecuperadorPoliticas recuperador = new RecuperadorPoliticas(embedderFijo(), almacen, RELOJ);

        assertThat(recuperador.buscar("¿cuál es el límite?", Set.of(), 3)).isEmpty();
        assertThat(recuperador.buscar("¿cuál es el límite?", Set.of("  "), 3)).isEmpty();
        assertThat(almacen.llamadas).isZero();
    }

    @Test
    void pasaRolesNormalizadosYFechaDeHoy() {
        AlmacenRegistrado almacen = new AlmacenRegistrado();
        RecuperadorPoliticas recuperador = new RecuperadorPoliticas(embedderFijo(), almacen, RELOJ);

        recuperador.buscar("límite", Set.of("compras", " direccion "), 3);

        assertThat(almacen.rolesRecibidos).containsExactlyInAnyOrder("COMPRAS", "DIRECCION");
        assertThat(almacen.hoyRecibido).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(almacen.limiteRecibido).isEqualTo(3);
    }

    @Test
    void limitaElNumeroDeResultados() {
        AlmacenRegistrado almacen = new AlmacenRegistrado();
        RecuperadorPoliticas recuperador = new RecuperadorPoliticas(embedderFijo(), almacen, RELOJ);

        recuperador.buscar("límite", Set.of("COMPRAS"), 500);

        assertThat(almacen.limiteRecibido).isEqualTo(10);
    }

    @Test
    void embebeLaPreguntaComoConsulta() {
        List<Embedder.TipoEntrada> tipos = new ArrayList<>();
        Embedder embedder = (textos, tipo) -> {
            tipos.add(tipo);
            return List.of(new float[]{1f});
        };
        RecuperadorPoliticas recuperador = new RecuperadorPoliticas(embedder, new AlmacenRegistrado(), RELOJ);

        recuperador.buscar("límite", Set.of("COMPRAS"), 3);

        assertThat(tipos).containsExactly(Embedder.TipoEntrada.CONSULTA);
    }

    private static Embedder embedderFijo() {
        return (textos, tipo) -> textos.stream().map(t -> new float[]{1f}).toList();
    }

    static final class AlmacenRegistrado implements AlmacenFragmentos {
        int llamadas;
        Set<String> rolesRecibidos;
        LocalDate hoyRecibido;
        int limiteRecibido;

        @Override
        public void reemplazar(String politicaId, List<FragmentoIndexado> fragmentos) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ResultadoPolitica> buscar(Set<String> roles, float[] consulta, LocalDate hoy, int limite) {
            llamadas++;
            rolesRecibidos = roles;
            hoyRecibido = hoy;
            limiteRecibido = limite;
            return List.of();
        }
    }
}
