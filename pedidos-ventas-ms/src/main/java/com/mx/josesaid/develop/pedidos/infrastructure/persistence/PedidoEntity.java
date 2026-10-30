package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import com.mx.josesaid.develop.pedidos.domain.Canal;
import com.mx.josesaid.develop.pedidos.domain.EstatusPedido;
import com.mx.josesaid.develop.pedidos.domain.MotivoCancelacion;
import com.mx.josesaid.develop.pedidos.domain.Pedido;
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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedidos")
public class PedidoEntity {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "cotizacion_id", unique = true)
    private UUID cotizacionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Canal canal;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TasaIva tasa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstatusPedido estatus;

    @Column(name = "fecha_confirmacion")
    private Instant fechaConfirmacion;

    @Column(name = "fecha_entrega")
    private Instant fechaEntrega;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_cancelacion", length = 25)
    private MotivoCancelacion motivoCancelacion;

    @Version
    private Long version;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "lineas_pedido", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicion")
    private List<LineaEmbebible> lineas = new ArrayList<>();

    protected PedidoEntity() {
    }

    static PedidoEntity nuevo(Pedido p) {
        PedidoEntity e = new PedidoEntity();
        e.id = p.getId();
        e.actualizar(p);
        return e;
    }

    void actualizar(Pedido p) {
        this.clienteId = p.getClienteId();
        this.cotizacionId = p.getCotizacionId();
        this.canal = p.getCanal();
        this.moneda = p.getMoneda();
        this.tasa = p.getTasa();
        this.estatus = p.getEstatus();
        this.fechaConfirmacion = p.getFechaConfirmacion();
        this.fechaEntrega = p.getFechaEntrega();
        this.motivoCancelacion = p.getMotivoCancelacion();
        this.lineas.clear();
        p.getLineas().forEach(l -> this.lineas.add(new LineaEmbebible(l)));
    }

    Pedido aDominio() {
        return Pedido.reconstruir(id, clienteId, cotizacionId, canal, moneda, tasa, estatus,
                lineas.stream().map(LineaEmbebible::aLinea).toList(),
                fechaConfirmacion, fechaEntrega, motivoCancelacion);
    }
}
