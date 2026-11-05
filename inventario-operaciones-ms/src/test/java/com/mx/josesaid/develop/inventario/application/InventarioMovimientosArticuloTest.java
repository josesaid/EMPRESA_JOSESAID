package com.mx.josesaid.develop.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.MovimientoRepositorio;
import com.mx.josesaid.develop.inventario.domain.RecursoNoEncontradoException;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventarioMovimientosArticuloTest {

    @Test
    void devuelveLosMovimientosDelArticulo() {
        Articulo articulo = Articulo.crear("SKU-A", "Tornillo", "pza", false, new BigDecimal("10"));
        MovimientoInventario movimiento = mock(MovimientoInventario.class);
        ArticuloRepositorio articulos = mock(ArticuloRepositorio.class);
        MovimientoRepositorio movimientos = mock(MovimientoRepositorio.class);
        when(articulos.buscarPorId(articulo.getId())).thenReturn(Optional.of(articulo));
        when(movimientos.listarRecientesDeArticulo(eq(articulo.getId()), eq(15))).thenReturn(List.of(movimiento));
        InventarioService servicio = new InventarioService(articulos, null, null, movimientos, null,
                mock(com.mx.josesaid.develop.inventario.application.PublicadorEventos.class), Clock.systemUTC());

        assertThat(servicio.movimientosDeArticulo(articulo.getId())).containsExactly(movimiento);
    }

    @Test
    void articuloInexistenteFalla() {
        ArticuloRepositorio articulos = mock(ArticuloRepositorio.class);
        UUID id = UUID.randomUUID();
        when(articulos.buscarPorId(id)).thenReturn(Optional.empty());
        InventarioService servicio = new InventarioService(articulos, null, null, mock(MovimientoRepositorio.class),
                null, mock(com.mx.josesaid.develop.inventario.application.PublicadorEventos.class), Clock.systemUTC());

        assertThatThrownBy(() -> servicio.movimientosDeArticulo(id))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }
}
