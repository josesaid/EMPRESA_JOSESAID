package com.mx.josesaid.develop.nexus.application;

import java.math.BigDecimal;
import java.util.List;

/** Lo que propone el modelo. Todavía no es una póliza: nadie la ha registrado. */
public record PolizaSugerida(String tipo, List<LineaSugerida> lineas, double confianza, String motivo) {

    public record LineaSugerida(String codigoCuenta, BigDecimal cargo, BigDecimal abono) {
    }
}
