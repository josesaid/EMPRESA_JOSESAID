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

    /** Cuenta del catálogo contable, usada como ejemplo para el modelo. */
    record CuentaCatalogo(UUID id, String codigo, String nombre) {
    }

    /** Póliza de ejemplo para que el modelo copie el formato correcto. */
    record PolizaEjemplo(String tipo, LocalDate fecha, List<LineaEjemplo> lineas) {
    }

    /** Línea de una póliza de ejemplo: cuenta, cargo y abono. */
    record LineaEjemplo(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
