package com.mx.josesaid.develop.nexus.application;

import java.util.List;

/**
 * Respuesta del asistente con los datos de consumo y herramientas para la auditoría.
 *
 * @param texto texto libre, de tipo {@code String}.
 * @param tokensEntrada tokens entrada, de tipo {@code Integer}.
 * @param tokensSalida tokens salida, de tipo {@code Integer}.
 * @param herramientas herramientas, de tipo {@code List<String>}.
 */
public record RespuestaAsistente(String texto, Integer tokensEntrada, Integer tokensSalida, List<String> herramientas) {

    public RespuestaAsistente {
        herramientas = herramientas == null ? List.of() : List.copyOf(herramientas);
    }

    /** Respuesta sin fuentes ni herramientas usadas. Se usa para respuestas de texto simple, como el rechazo de temas fuera de la empresa. */
    public RespuestaAsistente(String texto) {
        this(texto, null, null, List.of());
    }
}
