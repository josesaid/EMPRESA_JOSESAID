package com.mx.josesaid.develop.nexus.application;

import java.time.Instant;

/**
 * Datos de una consulta para auditoría. Los textos llegan ya redactados.
 *
 * @param usuario componente de tipo {@code String}.
 * @param preguntaRedactada componente de tipo {@code String}.
 * @param respuestaResumen componente de tipo {@code String}.
 * @param herramientas componente de tipo {@code String}.
 * @param tokensEntrada componente de tipo {@code Integer}.
 * @param tokensSalida componente de tipo {@code Integer}.
 * @param creadoUtc componente de tipo {@code Instant}.
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
