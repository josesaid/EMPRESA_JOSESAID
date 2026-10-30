package com.mx.josesaid.develop.usuarios_accesos.api;

/** Oculta datos personales en las respuestas de la API. Nunca se usa en logs: los logs no registran estos datos. */
public final class Enmascarado {

    private Enmascarado() {
    }

    /** {@code juan.perez@mail.com} se muestra como {@code ju***@mail.com}. */
    public static String correo(String correo) {
        if (correo == null) {
            return null;
        }
        int arroba = correo.indexOf('@');
        if (arroba <= 0) {
            return "***";
        }
        return correo.substring(0, Math.min(2, arroba)) + "***" + correo.substring(arroba);
    }

    /** {@code Juan Pérez} se muestra como {@code J***}. */
    public static String nombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return null;
        }
        return nombre.charAt(0) + "***";
    }
}
