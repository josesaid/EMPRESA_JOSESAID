package com.mx.josesaid.develop.facturacion_pagos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComprobanteTest {

    private static Comprobante borrador() {
        return Comprobante.crearBorrador(UUID.randomUUID(), "MXN", new BigDecimal("1000"), new BigDecimal("0.16"));
    }

    @Test
    void calculaIvaYTotalConRedondeoBancario() {
        Comprobante c = borrador();
        assertThat(c.getIva()).isEqualByComparingTo("160.00");
        assertThat(c.getTotal()).isEqualByComparingTo("1160.00");
        assertThat(c.getEstatus()).isEqualTo(EstatusComprobante.BORRADOR);
        assertThat(c.getFolio()).isNull();
    }

    @Test
    void rechazaMonedaNoPermitida() {
        assertThatThrownBy(() -> Comprobante.crearBorrador(UUID.randomUUID(), "EUR", BigDecimal.TEN, BigDecimal.ZERO))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void emitirAsignaFolioYNoSePuedeRepetir() {
        Comprobante c = borrador();
        c.emitir();
        assertThat(c.getEstatus()).isEqualTo(EstatusComprobante.EMITIDO);
        assertThat(c.getFolio()).startsWith("FAC-");
        assertThatThrownBy(c::emitir).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void cancelarExigeMotivoYSoloAplicaAEmitidos() {
        Comprobante c = borrador();
        assertThatThrownBy(() -> c.cancelar("x")).isInstanceOf(ReglaDeNegocioException.class);
        c.emitir();
        assertThatThrownBy(() -> c.cancelar("  ")).isInstanceOf(ReglaDeNegocioException.class);
        c.cancelar("Error en importe");
        assertThat(c.getEstatus()).isEqualTo(EstatusComprobante.CANCELADO);
        assertThat(c.getMotivoCancelacion()).isEqualTo("Error en importe");
    }

    @Test
    void pagosSoloEnEmitidosYNoExcedenElSaldo() {
        Comprobante c = borrador();
        assertThatThrownBy(() -> c.registrarPago(BigDecimal.ONE, LocalDate.now(), null))
                .isInstanceOf(ReglaDeNegocioException.class);
        c.emitir();
        c.registrarPago(new BigDecimal("500"), LocalDate.now(), "SPEI-1");
        assertThat(c.estatusCobro()).isEqualTo(EstatusCobro.PARCIAL);
        assertThat(c.saldo()).isEqualByComparingTo("660.00");
        assertThatThrownBy(() -> c.registrarPago(new BigDecimal("661"), LocalDate.now(), null))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("excede");
        c.registrarPago(new BigDecimal("660"), LocalDate.now(), null);
        assertThat(c.estatusCobro()).isEqualTo(EstatusCobro.PAGADO);
        assertThat(c.getPagos()).hasSize(2);
    }

    @Test
    void sinPagosElEstatusDeCobroEsPendiente() {
        Comprobante c = borrador();
        c.emitir();
        assertThat(c.estatusCobro()).isEqualTo(EstatusCobro.PENDIENTE);
    }
}
