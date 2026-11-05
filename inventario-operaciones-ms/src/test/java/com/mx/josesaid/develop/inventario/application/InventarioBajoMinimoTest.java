package com.mx.josesaid.develop.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.ArticuloBajoMinimo;
import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import com.mx.josesaid.develop.inventario.domain.ExistenciaRepositorio;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventarioBajoMinimoTest {

    @Test
    void listaSoloLosBajoMinimoOrdenadosPorFaltante() {
        Articulo a = Articulo.crear("SKU-A", "Tornillo", "pza", false, new BigDecimal("10"));
        Articulo b = Articulo.crear("SKU-B", "Tuerca", "pza", false, new BigDecimal("5"));
        Articulo c = Articulo.crear("SKU-C", "Rondana", "pza", false, new BigDecimal("20"));
        ArticuloRepositorio articulos = mock(ArticuloRepositorio.class);
        ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);
        when(articulos.listarConMinimo()).thenReturn(List.of(a, b, c));
        when(existencias.cantidadTotalPorArticulo()).thenReturn(Map.of(
                a.getId(), new BigDecimal("4"),
                b.getId(), new BigDecimal("8")));
        InventarioService servicio = new InventarioService(articulos, null, existencias, null, null,
                mock(PublicadorEventos.class), Clock.systemUTC());

        List<ArticuloBajoMinimo> resultado = servicio.listarBajoMinimo(15);

        assertThat(resultado).extracting(ArticuloBajoMinimo::sku).containsExactly("SKU-C", "SKU-A");
        assertThat(resultado.get(0).faltante()).isEqualByComparingTo("20");
        assertThat(resultado.get(1).faltante()).isEqualByComparingTo("6");
    }

    @Test
    void respetaElLimite() {
        Articulo a = Articulo.crear("SKU-A", "Tornillo", "pza", false, new BigDecimal("10"));
        Articulo c = Articulo.crear("SKU-C", "Rondana", "pza", false, new BigDecimal("20"));
        ArticuloRepositorio articulos = mock(ArticuloRepositorio.class);
        ExistenciaRepositorio existencias = mock(ExistenciaRepositorio.class);
        when(articulos.listarConMinimo()).thenReturn(List.of(a, c));
        when(existencias.cantidadTotalPorArticulo()).thenReturn(Map.of());
        InventarioService servicio = new InventarioService(articulos, null, existencias, null, null,
                mock(PublicadorEventos.class), Clock.systemUTC());

        assertThat(servicio.listarBajoMinimo(1)).extracting(ArticuloBajoMinimo::sku).containsExactly("SKU-C");
    }
}
