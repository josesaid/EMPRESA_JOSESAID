package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import com.mx.josesaid.develop.reportes.domain.Area;
import com.mx.josesaid.develop.reportes.domain.Indicador;
import com.mx.josesaid.develop.reportes.domain.IndicadorRepositorio;
import com.mx.josesaid.develop.reportes.domain.Periodo;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class IndicadorRepositorioJpa implements IndicadorRepositorio {

    private final IndicadorJpaRepository jpa;

    public IndicadorRepositorioJpa(IndicadorJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Indicador guardar(Indicador indicador) {
        IndicadorEntity e = jpa.findByAreaAndNombreAndPeriodo(indicador.area(), indicador.nombre(),
                        indicador.periodo().clave())
                .map(existente -> {
                    existente.actualizar(indicador);
                    return existente;
                })
                .orElseGet(() -> IndicadorEntity.nuevo(indicador));
        return jpa.save(e).aDominio();
    }

    @Override
    public List<Indicador> buscar(Area area, Periodo periodo) {
        Specification<IndicadorEntity> spec = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            condiciones.add(cb.equal(raiz.get("periodo"), periodo.clave()));
            if (area != null) {
                condiciones.add(cb.equal(raiz.get("area"), area));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return jpa.findAll(spec).stream().map(IndicadorEntity::aDominio).toList();
    }
}
