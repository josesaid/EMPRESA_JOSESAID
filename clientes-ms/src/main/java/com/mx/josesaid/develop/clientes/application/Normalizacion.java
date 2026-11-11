package com.mx.josesaid.develop.clientes.application;

import java.util.Locale;

/** Normalización de datos antes de guardarlos o compararlos. */
final class Normalizacion {

    /** Clase de utilidad; no se instancia. */
    private Normalizacion() {
    }

    /** RFC en mayúsculas y sin espacios. Devuelve null si el valor está vacío. */
    static String rfc(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim().toUpperCase(Locale.ROOT);
    }
}
