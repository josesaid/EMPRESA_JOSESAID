package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Mapeo JPA de un pago aplicado a un comprobante. */
@Entity
@Table(name = "pagos")
public class PagoEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comprobante_id", nullable = false)
    private ComprobanteEntity comprobante;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(length = 100)
    private String referencia;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    protected PagoEntity() {
    }

    PagoEntity(UUID id, ComprobanteEntity comprobante, BigDecimal monto, LocalDate fechaPago, String referencia) {
        this.id = id;
        this.comprobante = comprobante;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.referencia = referencia;
    }

    @PrePersist
    void alPersistir() {
        this.creadoUtc = Instant.now();
    }

    public UUID getId() { return id; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getReferencia() { return referencia; }
}
