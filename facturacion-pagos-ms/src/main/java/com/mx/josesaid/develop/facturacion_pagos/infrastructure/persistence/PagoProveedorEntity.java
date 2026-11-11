package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Mapeo JPA de un pago registrado contra una cuenta por pagar. */
@Entity
@Table(name = "pagos_proveedor")
public class PagoProveedorEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cuenta_id", nullable = false)
    private CuentaPorPagarEntity cuenta;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDate fechaPago;

    @Column(length = 100)
    private String referencia;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected PagoProveedorEntity() {
    }

    PagoProveedorEntity(UUID id, CuentaPorPagarEntity cuenta, BigDecimal monto, LocalDate fechaPago, String referencia) {
        this.id = id;
        this.cuenta = cuenta;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.referencia = referencia;
        this.creadoUtc = Instant.now();
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el monto. */
    public BigDecimal getMonto() { return monto; }
    /** Devuelve la fecha del pago. */
    public LocalDate getFechaPago() { return fechaPago; }
    /** Devuelve la referencia. */
    public String getReferencia() { return referencia; }

    /** Devuelve la cuenta a la que pertenece el pago. */
    public CuentaPorPagarEntity getCuenta() {
        return cuenta;
    }
}
