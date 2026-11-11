package com.mx.josesaid.develop.nexus.comun.prompts;

/**
 * Texto de un prompt con su nombre y versión.
 *
 * @param nombre nombre descriptivo, de tipo {@code String}.
 * @param version version, de tipo {@code int}.
 * @param texto texto libre, de tipo {@code String}.
 */
public record PromptVersionado(String nombre, int version, String texto) {
}
