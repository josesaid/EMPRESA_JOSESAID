package com.mx.josesaid.develop.reportes.domain;

import java.util.regex.Pattern;

/**
 * Periodo mensual (2026-10), trimestral (2026-Q4) o anual (2026).
 *
 * @param clave componente de tipo {@code String}.
 */
public record Periodo(String clave) {

    private static final Pattern FORMATO =
            Pattern.compile("^(\\d{4}-(0[1-9]|1[0-2])|\\d{4}-Q[1-4]|\\d{4})$");

    public Periodo {
        if (clave == null || !FORMATO.matcher(clave).matches()) {
            throw new SolicitudInvalidaException("Periodo inválido: use AAAA, AAAA-MM o AAAA-Qn");
        }
    }

    /** Crea un periodo a partir de su clave: AAAA-MM, AAAA-Qn o AAAA. Lanza excepción si el formato es inválido. */
    public static Periodo de(String clave) {
        return new Periodo(clave);
    }

    /** Devuelve la clave del periodo, por ejemplo 2026-09. */
    @Override
    public String toString() {
        return clave;
    }
}
