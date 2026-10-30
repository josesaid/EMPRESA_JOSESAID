package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import com.mx.josesaid.develop.reportes.domain.Periodo;
import com.mx.josesaid.develop.reportes.domain.Reporte;
import com.mx.josesaid.develop.reportes.domain.ReporteRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class ReporteRepositorioJpa implements ReporteRepositorio {

    private final ReporteJpaRepository jpa;

    public ReporteRepositorioJpa(ReporteJpaRepository jpa) {
        this.jpa = jpa;
    }

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

    @Override
    public Optional<Reporte> buscarPorPeriodo(Periodo periodo) {
        return jpa.findByPeriodo(periodo.clave()).map(ReporteEntity::aDominio);
    }

    @Override
    public Optional<Reporte> buscarPorId(UUID id) {
        return jpa.findById(id).map(ReporteEntity::aDominio);
    }
}
