package com.mx.josesaid.develop.ui.nexus;

import java.util.List;

/**
 * Datos de un alta pendiente que devuelve Nexus. No incluye contraseña.
 *
 * @param correo componente de tipo {@code String}.
 * @param nombreCompleto componente de tipo {@code String}.
 * @param roles componente de tipo {@code List<String>}.
 */
public record DatosAltaDto(String correo, String nombreCompleto, List<String> roles) {
}
