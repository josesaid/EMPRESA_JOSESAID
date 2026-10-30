package com.mx.josesaid.develop.nexus.application;

import java.time.Instant;

/** Datos de una consulta para auditoría. Los textos llegan ya redactados. */
public record RegistroConsulta(
        String usuario,
        String preguntaRedactada,
        String respuestaResumen,
        String herramientas,
        Integer tokensEntrada,
        Integer tokensSalida,
        Instant creadoUtc) {
}
