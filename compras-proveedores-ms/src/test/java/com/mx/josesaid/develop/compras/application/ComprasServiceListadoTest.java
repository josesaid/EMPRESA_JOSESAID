package com.mx.josesaid.develop.compras.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra;
import com.mx.josesaid.develop.compras.domain.OrdenCompraRepositorio;
import com.mx.josesaid.develop.compras.domain.Proveedor;
import com.mx.josesaid.develop.compras.domain.ProveedorRepositorio;
import com.mx.josesaid.develop.compras.domain.RecepcionRepositorio;
import com.mx.josesaid.develop.compras.domain.ResumenOrdenCompra;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComprasServiceListadoTest {

    private final OrdenCompraRepositorio ordenes = mock(OrdenCompraRepositorio.class);
    private final ProveedorRepositorio proveedores = mock(ProveedorRepositorio.class);
    private final ComprasService servicio = new ComprasService(proveedores, ordenes,
            mock(RecepcionRepositorio.class), mock(PublicadorEventos.class),
            new BigDecimal("18"), new BigDecimal("1000000"));

    @Test
    void listaConRazonSocialYLimiteFijo() {
        UUID proveedorId = UUID.randomUUID();
        UUID ordenId = UUID.randomUUID();
        Instant creada = Instant.parse("2026-11-05T12:00:00Z");
        when(ordenes.listarRecientes(eq(EstatusOrdenCompra.APROBADA), eq(15)))
                .thenReturn(List.of(new ResumenOrdenCompra(ordenId, proveedorId, EstatusOrdenCompra.APROBADA, creada)));
        when(proveedores.buscarPorId(proveedorId))
                .thenReturn(Optional.of(Proveedor.crear("Papelería del Centro", "PCE010101AB1", "ventas@pce.mx")));

        List<ComprasService.ResumenListado> lista = servicio.listarOrdenesRecientes(EstatusOrdenCompra.APROBADA);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).id()).isEqualTo(ordenId);
        assertThat(lista.get(0).razonSocialProveedor()).isEqualTo("Papelería del Centro");
        assertThat(lista.get(0).creadaUtc()).isEqualTo(creada);
        verify(ordenes).listarRecientes(EstatusOrdenCompra.APROBADA, ComprasService.LIMITE_LISTADO_ORDENES);
    }

    @Test
    void proveedorInexistenteDejaRazonSocialVacia() {
        UUID proveedorId = UUID.randomUUID();
        when(ordenes.listarRecientes(any(), eq(15)))
                .thenReturn(List.of(new ResumenOrdenCompra(UUID.randomUUID(), proveedorId,
                        EstatusOrdenCompra.BORRADOR, Instant.now())));
        when(proveedores.buscarPorId(proveedorId)).thenReturn(Optional.empty());

        List<ComprasService.ResumenListado> lista = servicio.listarOrdenesRecientes(null);

        assertThat(lista.get(0).razonSocialProveedor()).isEmpty();
    }
}
