package com.mx.josesaid.develop.facturacion_pagos.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Pago registrado contra una cuenta por pagar. Es inmutable: no se modifica ni se borra. */
public final class PagoProveedor {

    private final UUID id;
    private final BigDecimal monto;
    private final LocalDate fechaPago;
    private final String referencia;

    public PagoProveedor(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {
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
