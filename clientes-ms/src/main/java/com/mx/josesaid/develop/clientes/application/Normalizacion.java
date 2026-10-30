package com.mx.josesaid.develop.clientes.application;

import java.util.Locale;

final class Normalizacion {

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
