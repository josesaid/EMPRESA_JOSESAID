package com.mx.josesaid.develop.clientes.api;

/** Enmascarado de datos personales para respuestas. Los logs nunca usan estos valores. */
public final class Mascara {

    private Mascara() {
    }

    /** Muestra las tres primeras y las dos últimas posiciones. Ej.: IDE*******Y1 */
    public static String rfc(String rfc) {
        if (rfc == null) {
            return null;
        }
        int longitud = rfc.length();
        if (longitud <= 5) {
            return "*".repeat(longitud);
        }
        return rfc.substring(0, 3) + "*".repeat(longitud - 5) + rfc.substring(longitud - 2);
    }

    /** Muestra la primera letra y el dominio. Ej.: a***@ejemplo.mx */
    public static String correo(String correo) {
        if (correo == null) {
            return null;
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return correo.substring(0, 1) + "***" + correo.substring(arroba);
    }

    /** Muestra solo la primera letra. Ej.: A*** */
    public static String nombre(String nombre) {
        if (nombre == null) {
            return null;
        }
        String recortado = nombre.trim();
        if (recortado.isEmpty()) {
            return "";
        }
        return recortado.substring(0, 1) + "***";
    }
}
