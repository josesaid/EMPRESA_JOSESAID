package com.mx.josesaid.develop.nexus.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Lectura del catálogo y de pólizas históricas con el token del usuario. Nexus no escribe aquí. */
public interface ContextoContable {

    List<CuentaCatalogo> catalogo();

    /** Pólizas aplicadas recientes, usadas como ejemplos few-shot. */
    List<PolizaEjemplo> ejemplosRecientes();

    /**
     * Cuenta del catálogo contable, usada como ejemplo para el modelo.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param codigo código legible que identifica el registro, de tipo {@code String}.
     * @param nombre nombre descriptivo, de tipo {@code String}.
     */
    record CuentaCatalogo(UUID id, String codigo, String nombre) {
    }

    /**
     * Póliza de ejemplo para que el modelo copie el formato correcto.
     *
     * @param tipo tipo o clasificación del registro, de tipo {@code String}.
     * @param fecha fecha del registro, de tipo {@code LocalDate}.
     * @param lineas líneas o renglones del documento, de tipo {@code List<LineaEjemplo>}.
     */
    record PolizaEjemplo(String tipo, LocalDate fecha, List<LineaEjemplo> lineas) {
    }

    /**
     * Línea de una póliza de ejemplo: cuenta, cargo y abono.
     *
     * @param cuentaId identificador de la cuenta contable, de tipo {@code UUID}.
     * @param cargo importe del cargo, de tipo {@code BigDecimal}.
     * @param abono importe del abono, de tipo {@code BigDecimal}.
     */
    record LineaEjemplo(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
