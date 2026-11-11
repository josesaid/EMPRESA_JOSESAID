package com.mx.josesaid.develop.nexus.application;

/**
 * Fragmento recuperado para responder, con su fuente para poder citarla.
 *
 * @param politicaId componente de tipo {@code String}.
 * @param indice componente de tipo {@code int}.
 * @param titulo componente de tipo {@code String}.
 * @param texto componente de tipo {@code String}.
 * @param similitud componente de tipo {@code double}.
 */
public record ResultadoPolitica(String politicaId, int indice, String titulo, String texto, double similitud) {
}
