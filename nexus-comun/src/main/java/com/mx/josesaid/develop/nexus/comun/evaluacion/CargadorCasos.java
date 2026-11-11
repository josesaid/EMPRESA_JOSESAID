package com.mx.josesaid.develop.nexus.comun.evaluacion;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Lee los casos desde nexus/evaluaciones/casos.tsv (columnas separadas por tabulador). */
public final class CargadorCasos {

    static final String RECURSO = "nexus/evaluaciones/casos.tsv";

    /** Clase de utilidad: no se instancia. */
    private CargadorCasos() {
    }

    /** Lee los casos del recurso. Ignora líneas vacías y comentarios que empiezan con #. Cada línea necesita cuatro columnas: id, pregunta, "debe contener" y "no debe contener", separadas por tabulador; las listas van separadas por "|". Falla si el recurso no existe o una línea está incompleta. */
    public static List<CasoEvaluacion> cargar(ClassLoader cargador) {
        List<CasoEvaluacion> casos = new ArrayList<>();
        try (InputStream entrada = cargador.getResourceAsStream(RECURSO)) {
            if (entrada == null) {
                throw new IllegalStateException("Falta " + RECURSO);
            }
            try (BufferedReader lector = new BufferedReader(new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            String linea;
            while ((linea = lector.readLine()) != null) {
                if (linea.isBlank() || linea.startsWith("#")) {
                    continue;
                }
                String[] c = linea.split("\t", -1);
                if (c.length < 4) {
                    throw new IllegalStateException("Caso mal formado: " + linea);
                }
                casos.add(new CasoEvaluacion(c[0].trim(), c[1].trim(), lista(c[2]), lista(c[3])));
            }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer " + RECURSO, e);
        }
        return casos;
    }

    /** Convierte una celda separada por "|" en una lista sin espacios ni elementos vacíos. Una celda vacía produce una lista vacía. */
    static List<String> lista(String valor) {
        if (valor == null || valor.isBlank()) {
            return List.of();
        }
        return Arrays.stream(valor.split("\\|")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
