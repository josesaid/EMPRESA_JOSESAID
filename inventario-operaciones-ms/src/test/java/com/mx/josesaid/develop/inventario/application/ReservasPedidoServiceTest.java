package com.mx.josesaid.develop.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import com.mx.josesaid.develop.inventario.domain.Existencia;
import com.mx.josesaid.develop.inventario.domain.ExistenciaRepositorio;
import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.MovimientoRepositorio;
import com.mx.josesaid.develop.inventario.domain.ReservasPedidoRepositorio;
import com.mx.josesaid.develop.inventario.domain.TipoMovimiento;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReservasPedidoServiceTest {

    private final Map<UUID, Articulo> articulos = new HashMap<>();
    private final Map<String, Existencia> existencias = new HashMap<>();
    private final List<MovimientoInventario> movs = new ArrayList<>();
    private final Set<String> procesados = new HashSet<>();
    private final List<ReservasPedidoRepositorio.Reserva> reservasGuardadas = new ArrayList<>();
    private final Map<UUID, List<ReservasPedidoRepositorio.Reserva>> reservasPorPedido = new HashMap<>();
    private ReservasPedidoService servicio;

    private final UUID almacen = UUID.randomUUID();
    private final UUID pedido = UUID.randomUUID();
    private final Articulo tornillo = Articulo.crear("SKU-001", "Tornillo 3mm", "PZA", false, BigDecimal.ZERO);

    @BeforeEach
    void preparar() {
        articulos.put(tornillo.getId(), tornillo);
        ArticuloRepositorio ar = new ArticuloRepositorio() {
            public Articulo guardar(Articulo a) { articulos.put(a.getId(), a); return a; }
            public Optional<Articulo> buscarPorId(UUID id) { return Optional.ofNullable(articulos.get(id)); }
            public boolean existePorSku(String sku) { return false; }
            public Optional<Articulo> buscarPorSku(String sku) { return articulos.values().stream().filter(a -> sku.equals(a.getSku())).findFirst(); }
            public List<Articulo> listarConMinimo() { return List.of(); }
        };
        ExistenciaRepositorio exr = new ExistenciaRepositorio() {
            public Existencia guardar(Existencia e) { existencias.put(e.getArticuloId() + "|" + e.getAlmacenId(), e); return e; }
            public Optional<Existencia> buscar(UUID a, UUID l) { return Optional.ofNullable(existencias.get(a + "|" + l)); }
            public List<Existencia> buscarPorArticulo(UUID a) { return List.of(); }
            public Map<UUID, BigDecimal> cantidadTotalPorArticulo() { return Map.of(); }
        };
        MovimientoRepositorio mr = new MovimientoRepositorio() {
            public MovimientoInventario guardar(MovimientoInventario m) { movs.add(m); return m; }
            public List<MovimientoInventario> buscarPorLote(String lote) { return List.of(); }
            public List<MovimientoInventario> listarRecientesDeArticulo(UUID a, int limite) { return List.of(); }
        };
        ReservasPedidoRepositorio rr = new ReservasPedidoRepositorio() {
            public boolean marcarProcesado(UUID p, String evento) { return procesados.add(p + "|" + evento); }
            public void guardarReserva(UUID p, UUID a, UUID l, BigDecimal c) {
                ReservasPedidoRepositorio.Reserva r = new ReservasPedidoRepositorio.Reserva(a, l, c);
                reservasGuardadas.add(r);
                reservasPorPedido.computeIfAbsent(p, k -> new ArrayList<>()).add(r);
            }
            public List<Reserva> reservasDe(UUID p) { return reservasPorPedido.getOrDefault(p, List.of()); }
            public void eliminarReservas(UUID p) { reservasPorPedido.remove(p); }
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-11-09T21:00:00Z"), ZoneOffset.UTC);
        servicio = new ReservasPedidoService(ar, exr, mr, rr, reloj);
    }

    private void conExistencia(BigDecimal cantidad, BigDecimal reservado) {
        existencias.put(tornillo.getId() + "|" + almacen,
                Existencia.reconstruir(tornillo.getId(), almacen, cantidad, reservado));
    }

    private Existencia existencia() {
        return existencias.get(tornillo.getId() + "|" + almacen);
    }

    private ReservasPedidoService.LineaPedido linea(int cantidad) {
        return new ReservasPedidoService.LineaPedido(tornillo.getId(), BigDecimal.valueOf(cantidad));
    }

    @Test
    void alConfirmarReservaLoPedido() {
        conExistencia(new BigDecimal("100"), BigDecimal.ZERO);

        servicio.reservar(pedido, almacen, List.of(linea(10)));

        assertThat(existencia().getReservado()).isEqualByComparingTo("10");
        assertThat(existencia().disponible()).isEqualByComparingTo("90");
        assertThat(movs).singleElement().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimiento.RESERVA);
            assertThat(m.getCantidad()).isEqualByComparingTo("10");
        });
    }

    @Test
    void sinExistenciaSuficienteReservaLoQueAlcanza() {
        conExistencia(new BigDecimal("5"), BigDecimal.ZERO);

        servicio.reservar(pedido, almacen, List.of(linea(10)));

        assertThat(existencia().getReservado()).isEqualByComparingTo("5");
        assertThat(existencia().disponible()).isEqualByComparingTo("0");
    }

    @Test
    void sinExistenciaNoReservaNada() {
        conExistencia(new BigDecimal("5"), new BigDecimal("5"));

        servicio.reservar(pedido, almacen, List.of(linea(3)));

        assertThat(existencia().getReservado()).isEqualByComparingTo("5");
        assertThat(movs).isEmpty();
    }

    @Test
    void confirmarDosVecesNoDuplicaLaReserva() {
        conExistencia(new BigDecimal("100"), BigDecimal.ZERO);

        servicio.reservar(pedido, almacen, List.of(linea(10)));
        servicio.reservar(pedido, almacen, List.of(linea(10)));

        assertThat(existencia().getReservado()).isEqualByComparingTo("10");
        assertThat(movs).hasSize(1);
    }

    @Test
    void alEntregarDescuentaLoReservadoYLiberaLaReserva() {
        conExistencia(new BigDecimal("100"), BigDecimal.ZERO);
        servicio.reservar(pedido, almacen, List.of(linea(10)));

        servicio.entregar(pedido);

        assertThat(existencia().getCantidad()).isEqualByComparingTo("90");
        assertThat(existencia().getReservado()).isEqualByComparingTo("0");
        assertThat(existencia().disponible()).isEqualByComparingTo("90");
        assertThat(movs).extracting(MovimientoInventario::getTipo)
                .containsExactly(TipoMovimiento.RESERVA, TipoMovimiento.SALIDA);
    }

    @Test
    void entregarSinReservaNoTocaExistencias() {
        conExistencia(new BigDecimal("100"), BigDecimal.ZERO);

        servicio.entregar(pedido);

        assertThat(existencia().getCantidad()).isEqualByComparingTo("100");
        assertThat(movs).isEmpty();
    }

    @Test
    void entregarDosVecesNoDescuentaDosVeces() {
        conExistencia(new BigDecimal("100"), BigDecimal.ZERO);
        servicio.reservar(pedido, almacen, List.of(linea(10)));
        servicio.entregar(pedido);

        servicio.entregar(pedido);

        assertThat(existencia().getCantidad()).isEqualByComparingTo("90");
    }
}
