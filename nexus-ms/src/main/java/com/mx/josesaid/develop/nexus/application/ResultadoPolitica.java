package com.mx.josesaid.develop.nexus.application;

/**
 * Fragmento recuperado para responder, con su fuente para poder citarla.
 *
 * @param politicaId identificador del politica, de tipo {@code String}.
 * @param indice indice, de tipo {@code int}.
 * @param titulo título, de tipo {@code String}.
 * @param texto texto libre, de tipo {@code String}.
 * @param similitud similitud, de tipo {@code double}.
 */
public record ResultadoPolitica(String politicaId, int indice, String titulo, String texto, double similitud) {
}
