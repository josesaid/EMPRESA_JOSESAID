package com.mx.josesaid.develop.compras.infrastructure.persistence;

import com.mx.josesaid.develop.compras.domain.LineaOrden;
import com.mx.josesaid.develop.compras.domain.OrdenCompra;
import com.mx.josesaid.develop.compras.domain.OrdenCompraRepositorio;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
class OrdenCompraRepositorioJpa implements OrdenCompraRepositorio {

    private final OrdenCompraJpaRepository jpa;

    OrdenCompraRepositorioJpa(OrdenCompraJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Una orden nueva guarda sus líneas. Una existente actualiza estatus y cantidades recibidas. */
    @Override
    public OrdenCompra guardar(OrdenCompra o) {
        Optional<OrdenCompraEntity> existente = jpa.findById(o.getId());
        OrdenCompraEntity e;
        if (existente.isEmpty()) {
            e = new OrdenCompraEntity();
            e.id = o.getId();
            e.proveedorId = o.getProveedorId();
            e.moneda = o.getMoneda();
            for (LineaOrden l : o.getLineas()) {
                LineaOrdenEntity le = new LineaOrdenEntity();
                le.id = UUID.randomUUID();
                le.orden = e;
                le.articuloSku = l.getArticuloSku();
                le.cantidadOrdenada = l.getCantidadOrdenada();
                le.cantidadRecibida = l.getCantidadRecibida();
                le.precioUnitario = l.getPrecioUnitario();
                e.lineas.add(le);
            }
        } else {
            e = existente.get();
            for (LineaOrdenEntity le : e.lineas) {
                o.getLineas().stream()
                        .filter(l -> l.getArticuloSku().equals(le.articuloSku))
                        .findFirst()
                        .ifPresent(l -> le.cantidadRecibida = l.getCantidadRecibida());
            }
        }
        e.estatus = o.getEstatus();
        e.motivoCancelacion = o.getMotivoCancelacion();
        jpa.save(e);
        return o;
    }

    @Override
    public Optional<OrdenCompra> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> {
            List<LineaOrden> lineas = e.lineas.stream()
                    .map(le -> LineaOrden.reconstruir(le.articuloSku, le.cantidadOrdenada, le.cantidadRecibida,
                            le.precioUnitario))
                    .toList();
            return OrdenCompra.reconstruir(e.id, e.proveedorId, e.moneda, e.estatus, e.motivoCancelacion, lineas);
        });
    }
}
