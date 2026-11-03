package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import com.mx.josesaid.develop.contabilidad.domain.CuentaContable;
import com.mx.josesaid.develop.contabilidad.domain.CuentaRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class CuentaRepositorioJpa implements CuentaRepositorio {

    private final CuentaJpaRepository jpa;

    CuentaRepositorioJpa(CuentaJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CuentaContable guardar(CuentaContable c) {
        CuentaEntity e = new CuentaEntity();
        e.id = c.getId();
        e.codigo = c.getCodigo();
        e.nombre = c.getNombre();
        e.naturaleza = c.getNaturaleza();
        e.activa = c.isActiva();
        jpa.save(e);
        return c;
    }

    @Override
    public Optional<CuentaContable> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> CuentaContable.reconstruir(e.id, e.codigo, e.nombre, e.naturaleza, e.activa));
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        return jpa.existsByCodigo(codigo);
    }

    @Override
    public List<CuentaContable> listarActivas() {
        return jpa.findByActivaTrueOrderByCodigoAsc().stream()
                .map(e -> CuentaContable.reconstruir(e.id, e.codigo, e.nombre, e.naturaleza, e.activa))
                .toList();
    }
}
