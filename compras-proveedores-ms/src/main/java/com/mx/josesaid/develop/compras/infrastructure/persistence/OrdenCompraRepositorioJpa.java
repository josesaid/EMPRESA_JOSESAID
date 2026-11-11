package com.mx.josesaid.develop.compras.infrastructure.persistence;

import com.mx.josesaid.develop.compras.domain.LineaOrden;
import com.mx.josesaid.develop.compras.domain.OrdenCompra;
import com.mx.josesaid.develop.compras.domain.OrdenCompraRepositorio;
import com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra;
import com.mx.josesaid.develop.compras.domain.ResumenOrdenCompra;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Implementa el puerto de órdenes de compra con JPA. */
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
            e.creadaPor = o.getCreadaPor();
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

    /** Busca una orden por id. */
    @Override
    public Optional<OrdenCompra> buscarPorId(UUID id) {
        return jpa.findById(id).map(e -> {
            List<LineaOrden> lineas = e.lineas.stream()
                    .map(le -> LineaOrden.reconstruir(le.articuloSku, le.cantidadOrdenada, le.cantidadRecibida,
                            le.precioUnitario))
                    .toList();
            return OrdenCompra.reconstruir(e.id, e.proveedorId, e.moneda, e.estatus, e.motivoCancelacion, lineas,
                    e.creadaPor);
        });
    }

    /** Lista las órdenes más recientes, opcionalmente filtradas por estatus, con límite. */
    @Override
    public List<ResumenOrdenCompra> listarRecientes(EstatusOrdenCompra estatus, int limite) {
        var pagina = PageRequest.of(0, limite);
        List<OrdenCompraEntity> filas = estatus == null
                ? jpa.findAllByOrderByCreadoUtcDesc(pagina)
                : jpa.findByEstatusOrderByCreadoUtcDesc(estatus, pagina);
        return filas.stream()
                .map(e -> new ResumenOrdenCompra(e.id, e.proveedorId, e.estatus, e.creadoUtc))
                .toList();
    }
}
