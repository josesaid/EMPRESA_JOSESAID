package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AlmacenFragmentos;
import com.mx.josesaid.develop.nexus.application.Embedder;
import com.mx.josesaid.develop.nexus.application.FragmentoIndexado;
import com.mx.josesaid.develop.nexus.application.IngestaPoliticas;
import com.mx.josesaid.develop.nexus.application.RecuperadorPoliticas;
import com.mx.josesaid.develop.nexus.application.ResultadoPolitica;

/**
 * Evaluación de calidad con Voyage AI real: para cada pregunta, la regla correcta de la política
 * de compras debe estar entre los 3 primeros resultados (la herramienta entrega 5 fragmentos al modelo).
 * Solo corre con VOYAGE_API_KEY definida.
 */
@EnabledIfEnvironmentVariable(named = "VOYAGE_API_KEY", matches = ".+")
class EvaluacionRecuperacionPoliticasIT {

    private record Caso(String pregunta, String fragmentoEsperado) {
    }

    private static final List<Caso> CASOS = List.of(
            new Caso("¿Quién aprueba una orden de 500 mil dólares?", "la puede aprobar COMPRAS"),
            new Caso("¿Quién aprueba una orden de 2 millones de dólares?", "requiere aprobación de DIRECCION"),
            new Caso("¿Puedo aprobar una orden que yo mismo creé?", "él mismo creó"));

    @Test
    void laReglaCorrectaEstaEntreLasTresPrimeras() throws IOException {
        Embedder embedder = new VoyageEmbedder(RestClient.builder().baseUrl("https://api.voyageai.com").build(),
                System.getenv("VOYAGE_API_KEY"), "voyage-3");
        AlmacenEnMemoria almacen = new AlmacenEnMemoria();
        new IngestaPoliticas(embedder, almacen).ingerir(PoliticasClasspath.cargar());
        RecuperadorPoliticas recuperador = new RecuperadorPoliticas(embedder, almacen, Clock.systemUTC());

        for (Caso caso : CASOS) {
            List<String> textos = recuperador.buscar(caso.pregunta(), Set.of("COMPRAS"), 3).stream()
                    .map(ResultadoPolitica::texto)
                    .toList();
            assertThat(textos).as(caso.pregunta()).anyMatch(t -> t.contains(caso.fragmentoEsperado()));
        }
    }

    /** Almacén en memoria con el mismo criterio que la consulta SQL: rol en común y vigencia. */
    static final class AlmacenEnMemoria implements AlmacenFragmentos {
        private final List<FragmentoIndexado> guardados = new ArrayList<>();

        @Override
        public void reemplazar(String politicaId, List<FragmentoIndexado> fragmentos) {
            guardados.removeIf(f -> f.politicaId().equals(politicaId));
            guardados.addAll(fragmentos);
        }

        @Override
        public List<ResultadoPolitica> buscar(Set<String> roles, float[] consulta, LocalDate hoy, int limite) {
            return guardados.stream()
                    .filter(f -> f.roles().stream().anyMatch(roles::contains))
                    .filter(f -> !f.vigencia().isAfter(hoy))
                    .sorted(Comparator.comparingDouble((FragmentoIndexado f) -> similitud(f.vector(), consulta)).reversed())
                    .limit(limite)
                    .map(f -> new ResultadoPolitica(f.politicaId(), f.indice(), f.titulo(), f.texto(),
                            similitud(f.vector(), consulta)))
                    .toList();
        }

        private static double similitud(float[] a, float[] b) {
            double punto = 0;
            double normaA = 0;
            double normaB = 0;
            for (int i = 0; i < a.length; i++) {
                punto += a[i] * b[i];
                normaA += a[i] * a[i];
                normaB += b[i] * b[i];
            }
            return punto / (Math.sqrt(normaA) * Math.sqrt(normaB));
        }
    }
}
