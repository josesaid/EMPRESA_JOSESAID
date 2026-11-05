package com.mx.josesaid.develop.facturacion_pagos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CuentaPorPagarTest {

    private static final LocalDate HOY = LocalDate.of(2026, 11, 5);

    private static CuentaPorPagar cuenta(String total) {
        return CuentaPorPagar.crear(UUID.randomUUID(), UUID.randomUUID(), "MXN", new BigDecimal(total));
    }

    @Test
    void recienCreadaEstaPendienteConSaldoTotal() {
        CuentaPorPagar c = cuenta("1000");
        assertThat(c.estatus()).isEqualTo(EstatusCuentaPorPagar.PENDIENTE);
        assertThat(c.saldo()).isEqualByComparingTo("1000.00");
    }

    @Test
    void pagoParcialDejaLaCuentaParcial() {
        CuentaPorPagar c = cuenta("1000");
        c.registrarPago(new BigDecimal("400"), HOY, "SPEI-1");
        assertThat(c.estatus()).isEqualTo(EstatusCuentaPorPagar.PARCIAL);
        assertThat(c.saldo()).isEqualByComparingTo("600.00");
    }

    @Test
    void pagoTotalSaldaLaCuenta() {
        CuentaPorPagar c = cuenta("1000");
        c.registrarPago(new BigDecimal("1000"), HOY, null);
        assertThat(c.estatus()).isEqualTo(EstatusCuentaPorPagar.PAGADA);
        assertThat(c.saldo()).isEqualByComparingTo("0.00");
    }

    @Test
    void pagoQueExcedeElSaldoSeRechaza() {
        CuentaPorPagar c = cuenta("1000");
        assertThatThrownBy(() -> c.registrarPago(new BigDecimal("1000.01"), HOY, null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("excede el saldo");
    }

    @Test
    void montoNoPositivoOFechaFaltanteSeRechazan() {
        CuentaPorPagar c = cuenta("1000");
        assertThatThrownBy(() -> c.registrarPago(BigDecimal.ZERO, HOY, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> c.registrarPago(new BigDecimal("0.001"), HOY, null))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> c.registrarPago(new BigDecimal("10"), null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void monedaNoPermitidaSeRechaza() {
        assertThatThrownBy(() -> CuentaPorPagar.crear(UUID.randomUUID(), UUID.randomUUID(), "EUR", BigDecimal.TEN))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
