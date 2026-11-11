package com.mx.josesaid.develop.nexus.application;

import java.time.Instant;

/**
 * Datos de una consulta para auditoría. Los textos llegan ya redactados.
 *
 * @param usuario usuario, de tipo {@code String}.
 * @param preguntaRedactada pregunta redactada, de tipo {@code String}.
 * @param respuestaResumen respuesta resumen, de tipo {@code String}.
 * @param herramientas herramientas, de tipo {@code String}.
 * @param tokensEntrada tokens entrada, de tipo {@code Integer}.
 * @param tokensSalida tokens salida, de tipo {@code Integer}.
 * @param creadoUtc creado utc, de tipo {@code Instant}.
 */
public record RegistroConsulta(
        String usuario,
        String preguntaRedactada,
        String respuestaResumen,
        String herramientas,
        Integer tokensEntrada,
        Integer tokensSalida,
        Instant creadoUtc) {
}
