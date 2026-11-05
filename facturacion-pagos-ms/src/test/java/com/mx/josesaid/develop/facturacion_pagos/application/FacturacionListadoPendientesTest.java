package com.mx.josesaid.develop.facturacion_pagos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.facturacion_pagos.domain.ComprobantePendienteCobro;
import com.mx.josesaid.develop.facturacion_pagos.domain.ComprobanteRepositorio;
import com.mx.josesaid.develop.facturacion_pagos.domain.EstatusCobro;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FacturacionListadoPendientesTest {

    @Test
    void listaHastaQuinceComprobantesPendientes() {
        ComprobanteRepositorio repositorio = mock(ComprobanteRepositorio.class);
        ComprobantePendienteCobro pendiente = new ComprobantePendienteCobro(UUID.randomUUID(), "F-1", "MXN",
                new BigDecimal("1160.00"), new BigDecimal("1160.00"), EstatusCobro.PENDIENTE);
        when(repositorio.listarPendientesDeCobro(eq(15))).thenReturn(List.of(pendiente));
        FacturacionService servicio = new FacturacionService(repositorio, mock(PublicadorEventos.class),
                Clock.systemUTC());

        List<ComprobantePendienteCobro> resultado = servicio.listarPendientesDeCobro();

        verify(repositorio).listarPendientesDeCobro(15);
        assertThat(resultado).containsExactly(pendiente);
    }
}
