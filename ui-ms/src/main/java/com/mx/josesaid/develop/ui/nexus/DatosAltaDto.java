package com.mx.josesaid.develop.ui.nexus;

import java.util.List;

/**
 * Datos de un alta pendiente que devuelve Nexus. No incluye contraseña.
 *
 * @param correo correo electrónico de contacto, de tipo {@code String}.
 * @param nombreCompleto nombre completo, de tipo {@code String}.
 * @param roles roles asignados, de tipo {@code List<String>}.
 */
public record DatosAltaDto(String correo, String nombreCompleto, List<String> roles) {
}
