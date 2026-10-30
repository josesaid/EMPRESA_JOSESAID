package com.mx.josesaid.develop.reportes.application;

import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import java.util.List;
import java.util.UUID;

/** Datos que un exportador necesita. No depende de JPA ni de ningún formato. */
public record DatosExportacion(UUID reporteId, String periodo, List<IndicadorConsolidado> indicadores) {
}
