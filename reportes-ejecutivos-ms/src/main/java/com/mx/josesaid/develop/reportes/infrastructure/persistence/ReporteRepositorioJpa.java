package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import com.mx.josesaid.develop.reportes.domain.Periodo;
import com.mx.josesaid.develop.reportes.domain.Reporte;
import com.mx.josesaid.develop.reportes.domain.ReporteRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Implementa el puerto de reportes con JPA. */
@Repository
public class ReporteRepositorioJpa implements ReporteRepositorio {

    private final ReporteJpaRepository jpa;

    /** Recibe el repositorio JPA de reportes. */
    public ReporteRepositorioJpa(ReporteJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Guarda un reporte y devuelve la versión persistida. */
    @Override
    public Reporte guardar(Reporte reporte) {
        ReporteEntity e = jpa.findById(reporte.getId())
                .map(existente -> {
                    existente.actualizar(reporte);
                    return existente;
                })
                .orElseGet(() -> ReporteEntity.nuevo(reporte));
        return jpa.save(e).aDominio();
    }

    /** Busca un reporte por periodo. */
    @Override
    public Optional<Reporte> buscarPorPeriodo(Periodo periodo) {
        return jpa.findByPeriodo(periodo.clave()).map(ReporteEntity::aDominio);
    }

    /** Busca un reporte por identificador. */
    @Override
    public Optional<Reporte> buscarPorId(UUID id) {
        return jpa.findById(id).map(ReporteEntity::aDominio);
    }
}
