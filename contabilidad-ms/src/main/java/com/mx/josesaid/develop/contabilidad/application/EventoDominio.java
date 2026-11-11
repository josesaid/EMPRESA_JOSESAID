package com.mx.josesaid.develop.contabilidad.application;

import java.util.Map;

/**
 * Evento de negocio. Se guarda en el outbox y luego se envía a Kafka.
 *
 * @param tipo componente de tipo {@code String}.
 * @param agregadoId componente de tipo {@code String}.
 * @param datos componente de tipo {@code Map<String, Object>}.
 */
public record EventoDominio(String tipo, String agregadoId, Map<String, Object> datos) {
}
