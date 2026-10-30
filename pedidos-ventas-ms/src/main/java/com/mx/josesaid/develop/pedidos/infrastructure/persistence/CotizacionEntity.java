package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import com.mx.josesaid.develop.pedidos.domain.Canal;
import com.mx.josesaid.develop.pedidos.domain.Cotizacion;
import com.mx.josesaid.develop.pedidos.domain.EstatusCotizacion;
import com.mx.josesaid.develop.pedidos.domain.TasaIva;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "cotizaciones")
public class CotizacionEntity {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Canal canal;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TasaIva tasa;

    @Column(name = "vigente_hasta", nullable = false)
    private LocalDate vigenteHasta;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstatusCotizacion estatus;

    @Column(name = "pedido_id", unique = true)
    private UUID pedidoId;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lineas_cotizacion", joinColumns = @JoinColumn(name = "cotizacion_id"))
    @OrderColumn(name = "posicion")
    private List<LineaEmbebible> lineas = new ArrayList<>();

    protected CotizacionEntity() {
    }

    static CotizacionEntity nueva(Cotizacion c) {
        CotizacionEntity e = new CotizacionEntity();
        e.id = c.getId();
        e.actualizar(c);
        return e;
    }

    void actualizar(Cotizacion c) {
        this.clienteId = c.getClienteId();
        this.canal = c.getCanal();
        this.moneda = c.getMoneda();
        this.tasa = c.getTasa();
        this.vigenteHasta = c.getVigenteHasta();
        this.estatus = c.getEstatus();
        this.pedidoId = c.getPedidoId();
        this.lineas.clear();
        c.getLineas().forEach(l -> this.lineas.add(new LineaEmbebible(l)));
    }

    Cotizacion aDominio() {
        return Cotizacion.reconstruir(id, clienteId, canal, moneda, tasa, vigenteHasta,
                lineas.stream().map(LineaEmbebible::aLinea).toList(), estatus, pedidoId);
    }
}
