package com.mx.josesaid.develop.facturacion_pagos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mx.josesaid.develop.facturacion_pagos.domain.Comprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.ComprobanteRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.EstatusComprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FacturacionServiceTest {

    private final Map<UUID, Comprobante> almacen = new HashMap<>();
    private final List<EventoDominio> eventos = new ArrayList<>();
    private FacturacionService servicio;

    @BeforeEach
    void preparar() {
        ComprobanteRepositorio repo = new ComprobanteRepositorio() {
            @Override
            public Comprobante guardar(Comprobante c) {
                almacen.put(c.getId(), c);
                return c;
            }

            @Override
            public Optional<Comprobante> buscarPorId(UUID id) {
                return Optional.ofNullable(almacen.get(id));
            }

            @Override
            public Optional<Comprobante> buscarPorFolio(String folio) {
                return almacen.values().stream().filter(c -> folio.equals(c.getFolio())).findFirst();
            }
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-01-15T10:00:00Z"), ZoneOffset.UTC);
        servicio = new FacturacionService(repo, eventos::add, reloj);
    }

    @Test
    void flujoCompletoPublicaEventos() {
        Comprobante c = servicio.crearBorrador(UUID.randomUUID(), "MXN", new BigDecimal("100"), new BigDecimal("0.16"));
        servicio.emitir(c.getId());
        servicio.registrarPago(c.getId(), new BigDecimal("50"), null, "REF-1");
        servicio.cancelar(c.getId(), "Duplicado");

        assertThat(eventos).extracting(EventoDominio::tipo).containsExactly(
                FacturacionService.EVENTO_COMPROBANTE_EMITIDO,
                FacturacionService.EVENTO_PAGO_REGISTRADO,
                FacturacionService.EVENTO_COMPROBANTE_CANCELADO);
        assertThat(servicio.obtener(c.getId()).getEstatus()).isEqualTo(EstatusComprobante.CANCELADO);
    }

    @Test
    void pagoSinFechaUsaLaFechaDelReloj() {
        Comprobante c = servicio.crearBorrador(UUID.randomUUID(), "USD", new BigDecimal("10"), BigDecimal.ZERO);
        servicio.emitir(c.getId());
        Comprobante actualizado = servicio.registrarPago(c.getId(), BigDecimal.ONE, null, null);
        assertThat(actualizado.getPagos().get(0).getFechaPago()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void comprobanteInexistenteEsNoEncontrado() {
        assertThatThrownBy(() -> servicio.obtener(UUID.randomUUID()))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
