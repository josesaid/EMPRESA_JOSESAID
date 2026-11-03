package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class IngestaPoliticasTest {

    private static final String CUERPO = """
            # Política

            1. Toda orden la crea COMPRAS.

            2. Menos de 1,000,000 USD lo aprueba COMPRAS.
            """;

    @Test
    void indexaCadaReglaComoFragmentoConMetadatos() {
        Politica politica = new Politica("aprobacion", "Aprobación", LocalDate.of(2026, 11, 1),
                List.of("COMPRAS", "DIRECCION"), CUERPO);
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        IngestaPoliticas ingesta = new IngestaPoliticas(
                (textos, tipo) -> textos.stream().map(t -> new float[]{t.length()}).toList(), almacen);

        int total = ingesta.ingerir(List.of(politica));

        assertThat(total).isEqualTo(2);
        List<FragmentoIndexado> guardados = almacen.porPolitica.get("aprobacion");
        assertThat(guardados).hasSize(2);
        assertThat(guardados.get(0).indice()).isZero();
        assertThat(guardados.get(0).texto()).isEqualTo("1. Toda orden la crea COMPRAS.");
        assertThat(guardados.get(0).roles()).containsExactly("COMPRAS", "DIRECCION");
        assertThat(guardados.get(0).vigencia()).isEqualTo(LocalDate.of(2026, 11, 1));
    }

    @Test
    void embebeConTituloComoContexto() {
        Politica politica = new Politica("aprobacion", "Aprobación", LocalDate.of(2026, 11, 1),
                List.of("COMPRAS"), CUERPO);
        List<String> enviados = new ArrayList<>();
        IngestaPoliticas ingesta = new IngestaPoliticas((textos, tipo) -> {
            enviados.addAll(textos);
            assertThat(tipo).isEqualTo(Embedder.TipoEntrada.DOCUMENTO);
            return textos.stream().map(t -> new float[]{1f}).toList();
        }, new AlmacenEnMemoria());

        ingesta.ingerir(List.of(politica));

        assertThat(enviados).allMatch(t -> t.startsWith("Aprobación. "));
    }

    @Test
    void reemplazaFragmentosAlVolverAIngerir() {
        Politica politica = new Politica("aprobacion", "Aprobación", LocalDate.of(2026, 11, 1),
                List.of("COMPRAS"), CUERPO);
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        IngestaPoliticas ingesta = new IngestaPoliticas(
                (textos, tipo) -> textos.stream().map(t -> new float[]{1f}).toList(), almacen);

        ingesta.ingerir(List.of(politica));
        ingesta.ingerir(List.of(politica));

        assertThat(almacen.porPolitica.get("aprobacion")).hasSize(2);
    }

    /** Almacén en memoria: solo guarda, no filtra. El filtro por rol se prueba en AlmacenFragmentosJdbcIT. */
    static final class AlmacenEnMemoria implements AlmacenFragmentos {
        final Map<String, List<FragmentoIndexado>> porPolitica = new java.util.HashMap<>();

        @Override
        public void reemplazar(String politicaId, List<FragmentoIndexado> fragmentos) {
            porPolitica.put(politicaId, List.copyOf(fragmentos));
        }

        @Override
        public List<ResultadoPolitica> buscar(Set<String> roles, float[] consulta, LocalDate hoy, int limite) {
            throw new UnsupportedOperationException();
        }
    }
}
