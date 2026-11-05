package com.mx.josesaid.develop.facturacion_pagos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagar;
import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagarRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CuentasPorPagarServiceTest {

    private final CuentaPorPagarRepositorio repo = mock(CuentaPorPagarRepositorio.class);
    private final PublicadorEventos eventos = mock(PublicadorEventos.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-05T18:00:00Z"), ZoneOffset.UTC);
    private final CuentasPorPagarService servicio = new CuentasPorPagarService(repo, eventos, reloj);

    @Test
    void registrarCuentaNoDuplicaLaCuentaDeUnaOrden() {
        UUID orden = UUID.randomUUID();
        when(repo.buscarPorOrdenId(orden)).thenReturn(Optional.empty());
        servicio.registrarCuenta(orden, UUID.randomUUID(), "MXN", new BigDecimal("500"));
        verify(repo, times(1)).guardar(any(CuentaPorPagar.class));

        CuentaPorPagar existente = CuentaPorPagar.crear(orden, UUID.randomUUID(), "MXN", BigDecimal.TEN);
        when(repo.buscarPorOrdenId(orden)).thenReturn(Optional.of(existente));
        servicio.registrarCuenta(orden, UUID.randomUUID(), "MXN", new BigDecimal("500"));
        verify(repo, times(1)).guardar(any(CuentaPorPagar.class));
    }

    @Test
    void registrarPagoPublicaEventoConSaldoYFechaDelReloj() {
        CuentaPorPagar cuenta = CuentaPorPagar.crear(UUID.randomUUID(), UUID.randomUUID(), "MXN", new BigDecimal("1000"));
        when(repo.buscarPorId(cuenta.getId())).thenReturn(Optional.of(cuenta));
        when(repo.guardar(any(CuentaPorPagar.class))).thenAnswer(inv -> inv.getArgument(0));

        CuentaPorPagar guardada = servicio.registrarPago(cuenta.getId(), new BigDecimal("250"), null, "SPEI-7");

        assertThat(guardada.saldo()).isEqualByComparingTo("750.00");
        assertThat(guardada.getPagos().get(0).getFechaPago()).isEqualTo(LocalDate.of(2026, 11, 5));
        verify(eventos).publicar(any(EventoDominio.class));
    }
}
