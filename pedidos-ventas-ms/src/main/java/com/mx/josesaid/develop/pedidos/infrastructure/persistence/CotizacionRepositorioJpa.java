package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import com.mx.josesaid.develop.pedidos.domain.Cotizacion;
import com.mx.josesaid.develop.pedidos.domain.CotizacionRepositorio;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class CotizacionRepositorioJpa implements CotizacionRepositorio {

    private final CotizacionJpaRepository jpa;

    public CotizacionRepositorioJpa(CotizacionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Cotizacion guardar(Cotizacion cotizacion) {
        CotizacionEntity e = jpa.findById(cotizacion.getId())
                .map(existente -> {
                    existente.actualizar(cotizacion);
                    return existente;
                })
                .orElseGet(() -> CotizacionEntity.nueva(cotizacion));
        return jpa.save(e).aDominio();
    }

    @Override
    public Optional<Cotizacion> buscarPorId(UUID id) {
        return jpa.findById(id).map(CotizacionEntity::aDominio);
    }
}
