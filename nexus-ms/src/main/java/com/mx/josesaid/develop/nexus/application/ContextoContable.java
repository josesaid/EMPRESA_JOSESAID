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
     * @param id componente de tipo {@code UUID}.
     * @param codigo componente de tipo {@code String}.
     * @param nombre componente de tipo {@code String}.
     */
    record CuentaCatalogo(UUID id, String codigo, String nombre) {
    }

    /**
     * Póliza de ejemplo para que el modelo copie el formato correcto.
     *
     * @param tipo componente de tipo {@code String}.
     * @param fecha componente de tipo {@code LocalDate}.
     * @param lineas componente de tipo {@code List<LineaEjemplo>}.
     */
    record PolizaEjemplo(String tipo, LocalDate fecha, List<LineaEjemplo> lineas) {
    }

    /**
     * Línea de una póliza de ejemplo: cuenta, cargo y abono.
     *
     * @param cuentaId componente de tipo {@code UUID}.
     * @param cargo componente de tipo {@code BigDecimal}.
     * @param abono componente de tipo {@code BigDecimal}.
     */
    record LineaEjemplo(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
