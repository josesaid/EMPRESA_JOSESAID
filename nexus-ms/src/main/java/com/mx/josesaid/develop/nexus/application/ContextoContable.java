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

    record CuentaCatalogo(UUID id, String codigo, String nombre) {
    }

    record PolizaEjemplo(String tipo, LocalDate fecha, List<LineaEjemplo> lineas) {
    }

    record LineaEjemplo(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }
}
