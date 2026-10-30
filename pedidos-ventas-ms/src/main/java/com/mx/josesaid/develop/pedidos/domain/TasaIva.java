package com.mx.josesaid.develop.pedidos.domain;

import java.math.BigDecimal;

/** Tasas de IVA del catálogo. IVA_FRONTERIZO está pendiente de validación con contabilidad. */
public enum TasaIva {
    IVA_GENERAL(new BigDecimal("0.16")),
    IVA_FRONTERIZO(new BigDecimal("0.08")),
    IVA_EXENTO(BigDecimal.ZERO);

    private final BigDecimal valor;

    TasaIva(BigDecimal valor) {
        this.valor = valor;
    }

    public BigDecimal valor() {
        return valor;
    }
}
