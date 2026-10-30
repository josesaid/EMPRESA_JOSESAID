package com.mx.josesaid.develop.inventario.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mx.josesaid.develop.inventario.domain.*;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InventarioServiceTest {

    private final Map<UUID, Articulo> articulos = new HashMap<>();
    private final Map<UUID, Almacen> almacenes = new HashMap<>();
    private final Map<String, Existencia> existencias = new HashMap<>();
    private final List<MovimientoInventario> movs = new ArrayList<>();
    private final List<EventoDominio> eventos = new ArrayList<>();
    private InventarioService servicio;

    @BeforeEach
    void preparar() {
        ArticuloRepositorio ar = new ArticuloRepositorio() {
            public Articulo guardar(Articulo a) { articulos.put(a.getId(), a); return a; }
            public Optional<Articulo> buscarPorId(UUID id) { return Optional.ofNullable(articulos.get(id)); }
            public boolean existePorSku(String sku) { return articulos.values().stream().anyMatch(a -> a.getSku().equals(sku)); }
        };
        AlmacenRepositorio alr = new AlmacenRepositorio() {
            public Almacen guardar(Almacen a) { almacenes.put(a.getId(), a); return a; }
            public Optional<Almacen> buscarPorId(UUID id) { return Optional.ofNullable(almacenes.get(id)); }
        };
        ExistenciaRepositorio exr = new ExistenciaRepositorio() {
            public Existencia guardar(Existencia e) { existencias.put(e.getArticuloId() + "|" + e.getAlmacenId(), e); return e; }
            public Optional<Existencia> buscar(UUID a, UUID l) { return Optional.ofNullable(existencias.get(a + "|" + l)); }
            public List<Existencia> buscarPorArticulo(UUID a) {
                return existencias.values().stream().filter(e -> e.getArticuloId().equals(a)).toList();
            }
        };
        MovimientoRepositorio mr = new MovimientoRepositorio() {
            public MovimientoInventario guardar(MovimientoInventario m) { movs.add(m); return m; }
            public List<MovimientoInventario> buscarPorLote(String lote) {
                return movs.stream().filter(m -> lote.equals(m.getLote())).toList();
            }
        };
        OrdenRepositorio or = new OrdenRepositorio() {
            public OrdenOperacion guardar(OrdenOperacion o) { return o; }
            public Optional<OrdenOperacion> buscarPorId(UUID id) { return Optional.empty(); }
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC);
        servicio = new InventarioService(ar, alr, exr, mr, or, eventos::add, reloj);
    }

    @Test
    void articuloConLoteObligatorioExigeLote() {
        Articulo a = servicio.crearArticulo("SKU-1", "Tornillo", "PZA", true, BigDecimal.ZERO);
        Almacen l = servicio.crearAlmacen("Central", "Planta 1");
        assertThatThrownBy(() -> servicio.registrarMovimiento(a.getId(), l.getId(), TipoMovimiento.ENTRADA,
                BigDecimal.TEN, null, null)).isInstanceOf(SolicitudInvalidaException.class);
    }

    @Test
    void salidaQueCruzaElMinimoPublicaEventoDeBajoMinimo() {
        Articulo a = servicio.crearArticulo("SKU-2", "Resina", "KG", false, new BigDecimal("10"));
        Almacen l = servicio.crearAlmacen("Central", "Planta 1");
        servicio.registrarMovimiento(a.getId(), l.getId(), TipoMovimiento.ENTRADA, new BigDecimal("15"), null, null);
        assertThat(eventos).isEmpty();
        servicio.registrarMovimiento(a.getId(), l.getId(), TipoMovimiento.SALIDA, new BigDecimal("7"), null, null);
        assertThat(eventos).extracting(EventoDominio::tipo).containsExactly(InventarioService.EVENTO_BAJO_MINIMO);
    }

    @Test
    void articuloOAlmacenInexistenteEs404() {
        Almacen l = servicio.crearAlmacen("Central", "Planta 1");
        assertThatThrownBy(() -> servicio.registrarMovimiento(UUID.randomUUID(), l.getId(),
                TipoMovimiento.ENTRADA, BigDecimal.ONE, null, null)).isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void trazabilidadSinMovimientosEs404() {
        assertThatThrownBy(() -> servicio.trazabilidad("LOTE-X")).isInstanceOf(RecursoNoEncontradoException.class);
    }
}
