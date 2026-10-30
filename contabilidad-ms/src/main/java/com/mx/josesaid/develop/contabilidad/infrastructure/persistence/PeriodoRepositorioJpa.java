package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import com.mx.josesaid.develop.contabilidad.domain.Periodo;
import com.mx.josesaid.develop.contabilidad.domain.PeriodoRepositorio;
import java.time.YearMonth;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
class PeriodoRepositorioJpa implements PeriodoRepositorio {

    private final PeriodoJpaRepository jpa;

    PeriodoRepositorioJpa(PeriodoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Periodo guardar(Periodo p) {
        PeriodoEntity e = jpa.findById(p.getAnioMes().toString()).orElseGet(PeriodoEntity::new);
        e.anioMes = p.getAnioMes().toString();
        e.estatus = p.getEstatus();
        jpa.save(e);
        return p;
    }

    @Override
    public Optional<Periodo> buscarPorAnioMes(YearMonth anioMes) {
        return jpa.findById(anioMes.toString())
                .map(e -> Periodo.reconstruir(YearMonth.parse(e.anioMes), e.estatus));
    }
}
