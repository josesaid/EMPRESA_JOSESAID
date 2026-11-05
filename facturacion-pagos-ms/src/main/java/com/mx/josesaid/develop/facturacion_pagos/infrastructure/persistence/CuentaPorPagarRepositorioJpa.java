package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagar;
import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagarRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.PagoProveedor;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador que implementa el puerto de dominio con JPA. */
@Component
class CuentaPorPagarRepositorioJpa implements CuentaPorPagarRepositorio {

    private final CuentaPorPagarJpaRepository jpa;

    CuentaPorPagarRepositorioJpa(CuentaPorPagarJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public CuentaPorPagar guardar(CuentaPorPagar cuenta) {
        CuentaPorPagarEntity entidad = jpa.findById(cuenta.getId())
                .orElseGet(() -> new CuentaPorPagarEntity(cuenta.getId()));
        entidad.actualizarDesde(cuenta);
        return aDominio(jpa.save(entidad));
    }

    @Override
    public Optional<CuentaPorPagar> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<CuentaPorPagar> buscarPorOrdenId(UUID ordenId) {
        return jpa.findByOrdenId(ordenId).map(this::aDominio);
    }

    private CuentaPorPagar aDominio(CuentaPorPagarEntity e) {
        List<PagoProveedor> pagos = e.getPagos().stream()
                .map(p -> new PagoProveedor(p.getId(), p.getMonto(), p.getFechaPago(), p.getReferencia()))
                .toList();
        return CuentaPorPagar.reconstruir(e.getId(), e.getOrdenId(), e.getProveedorId(), e.getMoneda(),
                e.getMonto(), pagos);
    }
}
