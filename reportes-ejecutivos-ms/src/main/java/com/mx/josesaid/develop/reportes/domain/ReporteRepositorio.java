package com.mx.josesaid.develop.reportes.domain;

import java.util.Optional;
import java.util.UUID;

public interface ReporteRepositorio {
    Reporte guardar(Reporte reporte);
    Optional<Reporte> buscarPorPeriodo(Periodo periodo);
    Optional<Reporte> buscarPorId(UUID id);
}
