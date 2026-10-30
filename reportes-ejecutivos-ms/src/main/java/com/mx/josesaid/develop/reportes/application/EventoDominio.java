package com.mx.josesaid.develop.reportes.application;

import java.util.Map;

/** Evento de negocio. Se guarda en el outbox y luego se envía a Kafka. */
public record EventoDominio(String tipo, String agregadoId, Map<String, Object> datos) {
}
