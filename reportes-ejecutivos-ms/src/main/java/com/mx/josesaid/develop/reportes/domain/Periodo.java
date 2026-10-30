package com.mx.josesaid.develop.reportes.domain;

import java.util.regex.Pattern;

/** Periodo mensual (2026-10), trimestral (2026-Q4) o anual (2026). */
public record Periodo(String clave) {

    private static final Pattern FORMATO =
            Pattern.compile("^(\\d{4}-(0[1-9]|1[0-2])|\\d{4}-Q[1-4]|\\d{4})$");

    public Periodo {
        if (clave == null || !FORMATO.matcher(clave).matches()) {
            throw new SolicitudInvalidaException("Periodo inválido: use AAAA, AAAA-MM o AAAA-Qn");
        }
    }

    public static Periodo de(String clave) {
        return new Periodo(clave);
    }

    @Override
    public String toString() {
        return clave;
    }
}
