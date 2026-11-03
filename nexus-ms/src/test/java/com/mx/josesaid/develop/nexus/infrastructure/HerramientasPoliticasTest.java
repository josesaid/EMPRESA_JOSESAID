package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.mx.josesaid.develop.nexus.application.AlmacenFragmentos;
import com.mx.josesaid.develop.nexus.application.Embedder;
import com.mx.josesaid.develop.nexus.application.FragmentoIndexado;
import com.mx.josesaid.develop.nexus.application.RecuperadorPoliticas;
import com.mx.josesaid.develop.nexus.application.ResultadoPolitica;

class HerramientasPoliticasTest {

    /** Almacén que filtra por rol como lo hace la consulta SQL: basta un rol en común. */
    private final AlmacenFragmentos almacen = new AlmacenFragmentos() {
        private final List<FragmentoIndexado> guardados = new ArrayList<>();

        @Override
        public void reemplazar(String politicaId, List<FragmentoIndexado> fragmentos) {
            guardados.clear();
            guardados.addAll(fragmentos);
        }

        @Override
        public List<ResultadoPolitica> buscar(Set<String> roles, float[] consulta, LocalDate hoy, int limite) {
            return guardados.stream()
                    .filter(f -> f.roles().stream().anyMatch(roles::contains))
                    .map(f -> new ResultadoPolitica(f.politicaId(), f.indice(), f.titulo(), f.texto(), 1.0))
                    .toList();
        }
    };

    private HerramientasPoliticas herramienta;

    @BeforeEach
    void preparar() {
        Embedder embedder = (textos, tipo) -> textos.stream().map(t -> new float[]{1f}).toList();
        RecuperadorPoliticas recuperador = new RecuperadorPoliticas(embedder, almacen, Clock.systemUTC());
        almacen.reemplazar("aprobacion", List.of(new FragmentoIndexado("aprobacion", 0, "Aprobación",
                LocalDate.of(2026, 11, 1), List.of("COMPRAS", "DIRECCION"),
                "Menos de 1,000,000 USD lo aprueba COMPRAS.", new float[]{1f})));
        herramienta = new HerramientasPoliticas(recuperador);
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void devuelveElFragmentoConSuFuente() {
        comoRoles("ROLE_COMPRAS");

        String texto = herramienta.consultarPoliticas("¿Quién aprueba una orden chica?");

        assertThat(texto).contains("[Aprobación, regla 1]").contains("Menos de 1,000,000 USD");
    }

    @Test
    void unRolSinAccesoNoVeLaPolitica() {
        comoRoles("ROLE_VENTAS");

        String texto = herramienta.consultarPoliticas("¿Quién aprueba una orden chica?");

        assertThat(texto).doesNotContain("1,000,000").startsWith("No encontré");
    }

    @Test
    void sinAutenticacionNoDevuelveNada() {
        String texto = herramienta.consultarPoliticas("¿Quién aprueba una orden chica?");

        assertThat(texto).startsWith("No encontré");
    }

    private static void comoRoles(String... roles) {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("u1", "n/a", roles));
    }
}
