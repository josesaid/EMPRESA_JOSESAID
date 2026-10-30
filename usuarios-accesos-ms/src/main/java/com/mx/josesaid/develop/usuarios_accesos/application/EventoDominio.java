package com.mx.josesaid.develop.usuarios_accesos.application;

import java.util.Map;

/**
 * Evento de negocio emitido por la capa de aplicación.
 *
 * @param tipo       nombre del evento, por ejemplo {@code usuario-bloqueado}
 * @param agregadoId identificador del agregado, usado como clave de partición
 * @param datos      contenido del evento; no incluye datos personales en claro
 */
public record EventoDominio(String tipo, String agregadoId, Map<String, Object> datos) {
}
