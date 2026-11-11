package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagar;
import com.mx.josesaid.develop.facturacion_pagos.domain.PagoProveedor;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Mapeo JPA de la cuenta por pagar. Detalle de infraestructura, no de dominio. */
@Entity
@Table(name = "cuentas_por_pagar")
public class CuentaPorPagarEntity {

    @Id
    private UUID id;

    @Column(name = "orden_id", nullable = false, unique = true)
    private UUID ordenId;

    @Column(name = "proveedor_id", nullable = false)
    private UUID proveedorId;

    @Column(nullable = false, length = 3)
    private String moneda;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    @Version
    private Long version;

    @OneToMany(mappedBy = "cuenta", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PagoProveedorEntity> pagos = new ArrayList<>();

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected CuentaPorPagarEntity() {
    }

    CuentaPorPagarEntity(UUID id) {
        this.id = id;
        this.creadoUtc = Instant.now();
    }

    /** Actualiza la fila con los datos de la cuenta. */
    void actualizarDesde(CuentaPorPagar c) {
        this.ordenId = c.getOrdenId();
        this.proveedorId = c.getProveedorId();
        this.moneda = c.getMoneda();
        this.monto = c.getMonto();
        for (PagoProveedor p : c.getPagos()) {
            boolean yaGuardado = pagos.stream().anyMatch(e -> e.getId().equals(p.getId()));
            if (!yaGuardado) {
                pagos.add(new PagoProveedorEntity(p.getId(), this, p.getMonto(), p.getFechaPago(), p.getReferencia()));
            }
        }
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el id de la orden. */
    public UUID getOrdenId() { return ordenId; }
    /** Devuelve el id del proveedor. */
    public UUID getProveedorId() { return proveedorId; }
    /** Devuelve la moneda. */
    public String getMoneda() { return moneda; }
    /** Devuelve el monto. */
    public BigDecimal getMonto() { return monto; }
    /** Devuelve los pagos de la cuenta. */
    public List<PagoProveedorEntity> getPagos() { return pagos; }
}
