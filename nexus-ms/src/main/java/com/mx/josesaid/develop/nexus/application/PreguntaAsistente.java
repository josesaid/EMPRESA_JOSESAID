package com.mx.josesaid.develop.nexus.application;

/** Pregunta de un usuario autenticado. El usuario se usa para medir tokens y auditar. */
public record PreguntaAsistente(String usuario, String texto) {

    public PreguntaAsistente {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("La pregunta no puede estar vacía");
        }
    }
}
