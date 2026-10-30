package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import com.mx.josesaid.develop.contabilidad.domain.Movimiento;
import com.mx.josesaid.develop.contabilidad.domain.Poliza;
import com.mx.josesaid.develop.contabilidad.domain.PolizaRepositorio;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class PolizaRepositorioJpa implements PolizaRepositorio {

    private final PolizaJpaRepository jpa;

    PolizaRepositorioJpa(PolizaJpaRepository jpa) {
        this.jpa = jpa;
    }

    /**
     * Una póliza nueva guarda también sus movimientos. Una existente solo cambia su estatus:
     * los movimientos son inmutables.
     */
    @Override
    public Poliza guardar(Poliza p) {
        Optional<PolizaEntity> existente = jpa.findById(p.getId());
        PolizaEntity e = existente.orElseGet(PolizaEntity::new);
        if (existente.isEmpty()) {
            e.id = p.getId();
            e.anioMes = p.getPeriodo().toString();
            e.tipo = p.getTipo();
            e.fecha = p.getFecha();
            e.reversaDe = p.getReversaDe();
        }
        e.estatus = p.getEstatus();
        if (existente.isEmpty()) {
            for (Movimiento m : p.getMovimientos()) {
                MovimientoEntity me = new MovimientoEntity();
                me.id = UUID.randomUUID();
                me.poliza = e;
                me.cuentaId = m.getCuentaId();
                me.cargo = m.getCargo();
                me.abono = m.getAbono();
                e.movimientos.add(me);
            }
        }
        jpa.save(e);
        return p;
    }

    @Override
    public Optional<Poliza> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::aDominio);
    }

    @Override
    public List<Poliza> buscarPorPeriodo(YearMonth anioMes) {
        return jpa.findByAnioMes(anioMes.toString()).stream().map(this::aDominio).toList();
    }

    private Poliza aDominio(PolizaEntity e) {
        List<Movimiento> movs = e.movimientos.stream()
                .map(m -> new Movimiento(m.cuentaId, m.cargo, m.abono))
                .toList();
        return Poliza.reconstruir(e.id, YearMonth.parse(e.anioMes), e.tipo, e.fecha, e.estatus, e.reversaDe, movs);
    }
}
