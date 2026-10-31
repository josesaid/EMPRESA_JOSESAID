package com.mx.josesaid.develop.nexus.application;

import java.util.List;

/** Respuesta del asistente con los datos de consumo y herramientas para la auditoría. */
public record RespuestaAsistente(String texto, Integer tokensEntrada, Integer tokensSalida, List<String> herramientas) {

    public RespuestaAsistente {
        herramientas = herramientas == null ? List.of() : List.copyOf(herramientas);
    }

    public RespuestaAsistente(String texto) {
        this(texto, null, null, List.of());
    }
}
