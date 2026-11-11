package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import com.mx.josesaid.develop.facturacion_pagos.domain.Comprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.EstatusComprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.Pago;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Mapeo JPA del agregado Comprobante. Es un detalle de infraestructura, no de dominio. */
@Entity
@Table(name = "comprobantes")
public class ComprobanteEntity {

    @Id
    private UUID id;

    @Column(name = "orden_id", nullable = false)
    private UUID ordenId;

    @Column(length = 20, unique = true)
    private String folio;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal iva;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Column(name = "tasa_iva", nullable = false, precision = 5, scale = 4)
    private BigDecimal tasaIva;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstatusComprobante estatus;

    @Column(name = "motivo_cancelacion", length = 200)
    private String motivoCancelacion;

    @Version
    private Long version;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    @Column(name = "actualizado_utc", nullable = false)
    private Instant actualizadoUtc;

    @OneToMany(mappedBy = "comprobante", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("creadoUtc ASC")
    private List<PagoEntity> pagos = new ArrayList<>();

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected ComprobanteEntity() {
    }

    ComprobanteEntity(UUID id) {
        this.id = id;
        this.creadoUtc = Instant.now();
    }

    /** Copia el estado del dominio. Los pagos nuevos se agregan; los existentes no cambian. */
    void actualizarDesde(Comprobante c) {
        this.ordenId = c.getOrdenId();
        this.folio = c.getFolio();
        this.moneda = c.getMoneda();
        this.subtotal = c.getSubtotal();
        this.iva = c.getIva();
        this.total = c.getTotal();
        this.tasaIva = c.getTasaIva();
        this.estatus = c.getEstatus();
        this.motivoCancelacion = c.getMotivoCancelacion();
        this.actualizadoUtc = Instant.now();

        Set<UUID> existentes = pagos.stream().map(PagoEntity::getId).collect(Collectors.toSet());
        for (Pago p : c.getPagos()) {
            if (!existentes.contains(p.getId())) {
                pagos.add(new PagoEntity(p.getId(), this, p.getMonto(), p.getFechaPago(), p.getReferencia()));
            }
        }
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el id de la orden. */
    public UUID getOrdenId() { return ordenId; }
    /** Devuelve el folio. */
    public String getFolio() { return folio; }
    /** Devuelve la moneda. */
    public String getMoneda() { return moneda; }
    /** Devuelve el subtotal. */
    public BigDecimal getSubtotal() { return subtotal; }
    /** Devuelve el IVA. */
    public BigDecimal getIva() { return iva; }
    /** Devuelve el total. */
    public BigDecimal getTotal() { return total; }
    /** Devuelve la tasa de IVA. */
    public BigDecimal getTasaIva() { return tasaIva; }
    /** Devuelve el estatus. */
    public EstatusComprobante getEstatus() { return estatus; }
    /** Devuelve el motivo de cancelación. */
    public String getMotivoCancelacion() { return motivoCancelacion; }
    /** Devuelve los pagos del comprobante. */
    public List<PagoEntity> getPagos() { return pagos; }
}
