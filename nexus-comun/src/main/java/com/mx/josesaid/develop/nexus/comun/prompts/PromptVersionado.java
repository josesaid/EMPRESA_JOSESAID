package com.mx.josesaid.develop.nexus.comun.prompts;

/**
 * Texto de un prompt con su nombre y versión.
 *
 * @param nombre componente de tipo {@code String}.
 * @param version componente de tipo {@code int}.
 * @param texto componente de tipo {@code String}.
 */
public record PromptVersionado(String nombre, int version, String texto) {
}
