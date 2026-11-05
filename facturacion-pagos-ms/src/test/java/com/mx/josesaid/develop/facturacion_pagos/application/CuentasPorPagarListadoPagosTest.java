package com.mx.josesaid.develop.facturacion_pagos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagarRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.PagoResumen;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CuentasPorPagarListadoPagosTest {

    @Test
    void listaHastaQuinceArriba() {
        CuentaPorPagarRepositorio cuentas = mock(CuentaPorPagarRepositorio.class);
        PagoResumen pago = new PagoResumen(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("150.00"),
                LocalDate.of(2026, 11, 4), "SPEI-1");
        when(cuentas.listarPagosRecientes(eq(15))).thenReturn(List.of(pago));
        CuentasPorPagarService servicio = new CuentasPorPagarService(cuentas, mock(PublicadorEventos.class),
                Clock.systemUTC());

        List<PagoResumen> resultado = servicio.listarPagosRecientes();

        verify(cuentas).listarPagosRecientes(15);
        assertThat(resultado).containsExactly(pago);
    }
}
