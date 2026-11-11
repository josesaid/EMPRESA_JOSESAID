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

    /** Crea un pago registrado contra un comprobante. */
    public Pago(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {
        this.id = id;
        this.monto = monto;
        this.fechaPago = fechaPago;
        this.referencia = referencia;
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el monto. */
    public BigDecimal getMonto() { return monto; }
    /** Devuelve la fecha del pago. */
    public LocalDate getFechaPago() { return fechaPago; }
    /** Devuelve la referencia del pago. */
    public String getReferencia() { return referencia; }
}
