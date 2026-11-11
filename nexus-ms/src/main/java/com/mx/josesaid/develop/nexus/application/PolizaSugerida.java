package com.mx.josesaid.develop.nexus.application;

import java.math.BigDecimal;
import java.util.List;

/**
 * Lo que propone el modelo. Todavía no es una póliza: nadie la ha registrado.
 *
 * @param tipo tipo o clasificación del registro, de tipo {@code String}.
 * @param lineas líneas o renglones del documento, de tipo {@code List<LineaSugerida>}.
 * @param confianza confianza, de tipo {@code double}.
 * @param motivo motivo del registro o de la acción, de tipo {@code String}.
 */
public record PolizaSugerida(String tipo, List<LineaSugerida> lineas, double confianza, String motivo) {

    /**
     * Línea sugerida de una póliza: código de cuenta, cargo y abono.
     *
     * @param codigoCuenta codigo cuenta, de tipo {@code String}.
     * @param cargo importe del cargo, de tipo {@code BigDecimal}.
     * @param abono importe del abono, de tipo {@code BigDecimal}.
     */
    public record LineaSugerida(String codigoCuenta, BigDecimal cargo, BigDecimal abono) {
    }
}
