package com.mx.josesaid.develop.nexus.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que propone el modelo. Todavía no es una póliza: nadie la ha registrado.
 *
 * @param tipo componente de tipo {@code String}.
 * @param lineas componente de tipo {@code List<LineaSugerida>}.
 * @param confianza componente de tipo {@code double}.
 * @param motivo componente de tipo {@code String}.
 */
public record PolizaSugerida(String tipo, List<LineaSugerida> lineas, double confianza, String motivo) {

    /**
     * Línea sugerida de una póliza: código de cuenta, cargo y abono.
     *
     * @param codigoCuenta componente de tipo {@code String}.
     * @param cargo componente de tipo {@code BigDecimal}.
     * @param abono componente de tipo {@code BigDecimal}.
     */
    public record LineaSugerida(String codigoCuenta, BigDecimal cargo, BigDecimal abono) {
    }
}
