package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagar;
import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagarRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.PagoProveedor;
import com.mx.josesaid.develop.facturacion_pagos.domain.PagoResumen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Adaptador que implementa el puerto de dominio con JPA. */
@Component
class CuentaPorPagarRepositorioJpa implements CuentaPorPagarRepositorio {

    private final CuentaPorPagarJpaRepository jpa;
    private final PagoProveedorJpaRepository pagos;

    CuentaPorPagarRepositorioJpa(CuentaPorPagarJpaRepository jpa, PagoProveedorJpaRepository pagos) {
        this.jpa = jpa;
        this.pagos = pagos;
    }

    /** Devuelve los pagos más recientes con su cuenta, con límite. */
    @Override
    public List<PagoResumen> listarPagosRecientes(int limite) {
        return pagos.findAllByOrderByFechaPagoDescCreadoUtcDesc(PageRequest.of(0, limite)).stream()
                .map(p -> new PagoResumen(p.getId(), p.getCuenta().getId(), p.getMonto(), p.getFechaPago(),
                        p.getReferencia()))
                .toList();
    }

    /** Guarda una cuenta con sus pagos y devuelve la guardada. */
    @Override
    public CuentaPorPagar guardar(CuentaPorPagar cuenta) {
        CuentaPorPagarEntity entidad = jpa.findById(cuenta.getId())
                .orElseGet(() -> new CuentaPorPagarEntity(cuenta.getId()));
        entidad.actualizarDesde(cuenta);
        return aDominio(jpa.save(entidad));
    }

    /** Busca una cuenta por id. */
    @Override
    public Optional<CuentaPorPagar> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::aDominio);
    }

    /** Busca la cuenta de una orden de compra. */
    @Override
    public Optional<CuentaPorPagar> buscarPorOrdenId(UUID ordenId) {
        return jpa.findByOrdenId(ordenId).map(this::aDominio);
    }

    /** Convierte la fila en la cuenta del dominio. */
    private CuentaPorPagar aDominio(CuentaPorPagarEntity e) {
        List<PagoProveedor> pagos = e.getPagos().stream()
                .map(p -> new PagoProveedor(p.getId(), p.getMonto(), p.getFechaPago(), p.getReferencia()))
                .toList();
        return CuentaPorPagar.reconstruir(e.getId(), e.getOrdenId(), e.getProveedorId(), e.getMoneda(),
                e.getMonto(), pagos);
    }
}
