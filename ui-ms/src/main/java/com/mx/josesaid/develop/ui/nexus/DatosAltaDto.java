package com.mx.josesaid.develop.ui.nexus;

import java.util.List;

/** Datos de un alta pendiente que devuelve Nexus. No incluye contraseña. */
public record DatosAltaDto(String correo, String nombreCompleto, List<String> roles) {
}
