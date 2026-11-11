package com.mx.josesaid.develop.clientes.application;

import java.util.UUID;

/**
 * Posible duplicado encontrado: criterio ("correo", "nombre" o "razon_social"), tipo de registro e identificador.
 *
 * @param criterio componente de tipo {@code String}.
 * @param tipoRegistro componente de tipo {@code String}.
 * @param id componente de tipo {@code UUID}.
 */
public record Coincidencia(String criterio, String tipoRegistro, UUID id) {
}
