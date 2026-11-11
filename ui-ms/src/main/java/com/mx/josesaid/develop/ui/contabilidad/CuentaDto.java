package com.mx.josesaid.develop.ui.contabilidad;

import java.util.UUID;

/**
 * Forma de una cuenta contable tal como la devuelve contabilidad-ms.
 *
 * @param id identificador único del registro, de tipo {@code UUID}.
 * @param codigo código legible que identifica el registro, de tipo {@code String}.
 * @param nombre nombre descriptivo, de tipo {@code String}.
 * @param naturaleza naturaleza, de tipo {@code String}.
 * @param activa activa, de tipo {@code boolean}.
 */
public record CuentaDto(UUID id, String codigo, String nombre, String naturaleza, boolean activa) {
}
