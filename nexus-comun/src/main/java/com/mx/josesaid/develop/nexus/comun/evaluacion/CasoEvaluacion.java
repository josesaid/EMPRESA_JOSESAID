package com.mx.josesaid.develop.nexus.comun.evaluacion;

import java.util.List;

/** Caso de evaluación: la respuesta debe contener ciertas palabras y no otras. */
public record CasoEvaluacion(String id, String pregunta, List<String> debeContener, List<String> noDebeContener) {

    public CasoEvaluacion {
        debeContener = List.copyOf(debeContener);
        noDebeContener = List.copyOf(noDebeContener);
    }
}
