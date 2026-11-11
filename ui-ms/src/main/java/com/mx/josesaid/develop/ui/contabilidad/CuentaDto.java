package com.mx.josesaid.develop.ui.contabilidad;

import java.util.UUID;

/**
 * Forma de una cuenta contable tal como la devuelve contabilidad-ms.
 *
 * @param id componente de tipo {@code UUID}.
 * @param codigo componente de tipo {@code String}.
 * @param nombre componente de tipo {@code String}.
 * @param naturaleza componente de tipo {@code String}.
 * @param activa componente de tipo {@code boolean}.
 */
public record CuentaDto(UUID id, String codigo, String nombre, String naturaleza, boolean activa) {
}
