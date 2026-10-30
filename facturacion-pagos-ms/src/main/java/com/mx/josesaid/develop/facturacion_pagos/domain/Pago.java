package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Pago aplicado a un comprobante. Es inmutable: no se modifica ni se borra. */
public final class Pago {

    private final UUID id;
    private final BigDecimal monto;
    private final LocalDate fechaPago;
    private final String referencia;

    public Pago(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {
        this.id = id;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.referencia = referencia;
    }

    public UUID getId() { return id; }
    public BigDecimal getMonto() { return monto; }
    public LocalDate getFechaPago() { return fechaPago; }
    public String getReferencia() { return referencia; }
}
