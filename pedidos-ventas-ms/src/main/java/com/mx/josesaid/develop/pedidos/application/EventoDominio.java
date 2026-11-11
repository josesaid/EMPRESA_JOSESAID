package com.mx.josesaid.develop.pedidos.application;

import java.util.Map;

/**
 * Evento de negocio. Se guarda en el outbox y luego se envía a Kafka.
 *
 * @param tipo tipo o clasificación del registro, de tipo {@code String}.
 * @param agregadoId identificador del agregado de dominio al que pertenece el evento, de tipo {@code String}.
 * @param datos datos específicos de la operación, de tipo {@code Map<String, Object>}.
 */
public record EventoDominio(String tipo, String agregadoId, Map<String, Object> datos) {
}
