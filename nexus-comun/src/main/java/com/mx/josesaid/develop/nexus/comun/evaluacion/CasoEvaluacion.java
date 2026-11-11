package com.mx.josesaid.develop.nexus.comun.evaluacion;

import java.util.List;

/**
 * Caso de evaluación: la respuesta debe contener ciertas palabras y no otras.
 *
 * @param id identificador único del registro, de tipo {@code String}.
 * @param pregunta pregunta, de tipo {@code String}.
 * @param debeContener debe contener, de tipo {@code List<String>}.
 * @param noDebeContener no debe contener, de tipo {@code List<String>}.
 */
public record CasoEvaluacion(String id, String pregunta, List<String> debeContener, List<String> noDebeContener) {

    public CasoEvaluacion {
        debeContener = List.copyOf(debeContener);
        noDebeContener = List.copyOf(noDebeContener);
    }
}
