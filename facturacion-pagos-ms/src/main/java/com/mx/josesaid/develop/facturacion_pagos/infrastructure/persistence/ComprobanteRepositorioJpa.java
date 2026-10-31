package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import com.mx.josesaid.develop.facturacion_pagos.domain.Comprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.ComprobanteRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.Pago;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adaptador que implementa el puerto de dominio con JPA. */
@Component
class ComprobanteRepositorioJpa implements ComprobanteRepositorio {

    private final ComprobanteJpaRepository jpa;

    ComprobanteRepositorioJpa(ComprobanteJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Comprobante guardar(Comprobante comprobante) {
        ComprobanteEntity entidad = jpa.findById(comprobante.getId())
                .orElseGet(() -> new ComprobanteEntity(comprobante.getId()));
        entidad.actualizarDesde(comprobante);
        return aDominio(jpa.save(entidad));
    }

    @Override
    public Optional<Comprobante> buscarPorId(UUID id) {
        return jpa.findById(id).map(this::aDominio);
    }

    @Override
    public Optional<Comprobante> buscarPorFolio(String folio) {
        return jpa.findByFolio(folio).map(this::aDominio);
    }

    private Comprobante aDominio(ComprobanteEntity e) {
        List<Pago> pagos = e.getPagos().stream()
                .map(p -> new Pago(p.getId(), p.getMonto(), p.getFechaPago(), p.getReferencia()))
                .toList();
        return Comprobante.reconstruir(e.getId(), e.getOrdenId(), e.getMoneda(), e.getSubtotal(), e.getIva(),
                e.getTotal(), e.getTasaIva(), e.getFolio(), e.getEstatus(), e.getMotivoCancelacion(), pagos);
    }
}
