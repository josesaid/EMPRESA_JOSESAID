package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import com.mx.josesaid.develop.contabilidad.domain.CuentaContable;
import com.mx.josesaid.develop.contabilidad.domain.CuentaRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Implementa el puerto de cuentas con JPA. */
@Component
class CuentaRepositorioJpa implements CuentaRepositorio {

    private final CuentaJpaRepository jpa;

    CuentaRepositorioJpa(CuentaJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Guarda una cuenta y devuelve la guardada. */
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

    /** Busca una cuenta por id. */
    @Override
    public Optional<CuentaContable> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> CuentaContable.reconstruir(e.id, e.codigo, e.nombre, e.naturaleza, e.activa));
    }

    /** Indica si ya existe una cuenta con ese código. */
    @Override
    public boolean existePorCodigo(String codigo) {
        return jpa.existsByCodigo(codigo);
    }

    /** Lista las cuentas activas. */
    @Override
    public List<CuentaContable> listarActivas() {
        return jpa.findByActivaTrueOrderByCodigoAsc().stream()
                .map(e -> CuentaContable.reconstruir(e.id, e.codigo, e.nombre, e.naturaleza, e.activa))
                .toList();
    }
}
