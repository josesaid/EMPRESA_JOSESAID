package com.mx.josesaid.develop.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import com.mx.josesaid.develop.inventario.domain.Existencia;
import com.mx.josesaid.develop.inventario.domain.ExistenciaRepositorio;
import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.MovimientoRepositorio;
import com.mx.josesaid.develop.inventario.domain.RecepcionesRepositorio;
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

class RecepcionesServiceTest {

    private final Map<UUID, Articulo> articulos = new HashMap<>();
    private final Map<String, Existencia> existencias = new HashMap<>();
    private final List<MovimientoInventario> movs = new ArrayList<>();
    private final Set<UUID> eventos = new HashSet<>();
    private RecepcionesService servicio;

    private final UUID almacen = UUID.randomUUID();
    private final UUID orden = UUID.randomUUID();
    private final Articulo tornillo = Articulo.crear("SKU-001", "Tornillo 3mm", "PZA", false, BigDecimal.ZERO);
    private final Articulo conLote = Articulo.crear("SKU-LOTE", "Resina", "KG", true, BigDecimal.ZERO);

    @BeforeEach
    void preparar() {
        articulos.put(tornillo.getId(), tornillo);
        articulos.put(conLote.getId(), conLote);
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
        RecepcionesRepositorio rr = eventoId -> eventos.add(eventoId);
        Clock reloj = Clock.fixed(Instant.parse("2026-11-09T21:00:00Z"), ZoneOffset.UTC);
        servicio = new RecepcionesService(ar, exr, mr, rr, reloj);
    }

    private void conExistencia(Articulo a, BigDecimal cantidad) {
        existencias.put(a.getId() + "|" + almacen, Existencia.reconstruir(a.getId(), almacen, cantidad, BigDecimal.ZERO));
    }

    private Existencia existencia(Articulo a) {
        return existencias.get(a.getId() + "|" + almacen);
    }

    @Test
    void recepcionSumaALasExistencias() {
        conExistencia(tornillo, new BigDecimal("90"));

        servicio.registrar(UUID.randomUUID(), orden, almacen,
                List.of(new RecepcionesService.LineaRecibida("SKU-001", new BigDecimal("20"))));

        assertThat(existencia(tornillo).getCantidad()).isEqualByComparingTo("110");
        assertThat(movs).singleElement().satisfies(m -> {
            assertThat(m.getTipo()).isEqualTo(TipoMovimiento.ENTRADA);
            assertThat(m.getCantidad()).isEqualByComparingTo("20");
        });
    }

    @Test
    void mismoEventoNoSumaDosVeces() {
        conExistencia(tornillo, new BigDecimal("90"));
        UUID evento = UUID.randomUUID();
        var linea = new RecepcionesService.LineaRecibida("SKU-001", new BigDecimal("20"));

        servicio.registrar(evento, orden, almacen, List.of(linea));
        servicio.registrar(evento, orden, almacen, List.of(linea));

        assertThat(existencia(tornillo).getCantidad()).isEqualByComparingTo("110");
        assertThat(movs).hasSize(1);
    }

    @Test
    void skuDesconocidoSeOmite() {
        conExistencia(tornillo, new BigDecimal("90"));

        servicio.registrar(UUID.randomUUID(), orden, almacen,
                List.of(new RecepcionesService.LineaRecibida("NO-EXISTE", new BigDecimal("5"))));

        assertThat(existencia(tornillo).getCantidad()).isEqualByComparingTo("90");
        assertThat(movs).isEmpty();
    }

    @Test
    void articuloConLoteObligatorioSeOmite() {
        conExistencia(conLote, new BigDecimal("10"));

        servicio.registrar(UUID.randomUUID(), orden, almacen,
                List.of(new RecepcionesService.LineaRecibida("SKU-LOTE", new BigDecimal("5"))));

        assertThat(existencia(conLote).getCantidad()).isEqualByComparingTo("10");
        assertThat(movs).isEmpty();
    }
}
