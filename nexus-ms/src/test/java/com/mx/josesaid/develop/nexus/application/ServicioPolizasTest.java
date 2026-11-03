package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

import org.junit.jupiter.api.Test;

import com.mx.josesaid.develop.nexus.application.ContextoContable.CuentaCatalogo;
import com.mx.josesaid.develop.nexus.application.PolizaSugerida.LineaSugerida;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/** Verifica que Nexus sugiere pólizas pero no las registra por sí solo. */
class ServicioPolizasTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC);
    private static final UUID CAJA = UUID.fromString("00000000-0000-4000-8000-000000000101");
    private static final UUID VENTAS = UUID.fromString("00000000-0000-4000-8000-000000000401");
    private static final List<CuentaCatalogo> CATALOGO = List.of(
            new CuentaCatalogo(CAJA, "101", "Caja"),
            new CuentaCatalogo(VENTAS, "401", "Ventas"));

    private final Map<UUID, AccionPendiente> almacenadas = new HashMap<>();
    private final List<String> ejecuciones = new ArrayList<>();
    private final AccionesPendientes acciones = new AccionesPendientes(new AlmacenAcciones() {
        @Override
        public void guardar(AccionPendiente accion) {
            almacenadas.put(accion.id(), accion);
        }

        @Override
        public Optional<AccionPendiente> buscar(UUID id) {
            return Optional.ofNullable(almacenadas.get(id));
        }
    }, (tipo, payload) -> {
        ejecuciones.add(tipo + " " + payload);
        return "ok";
    }, RELOJ);

    private static final PolizaSugerida VALIDA = new PolizaSugerida("INGRESO", List.of(
            new LineaSugerida("101", new BigDecimal("1000"), BigDecimal.ZERO),
            new LineaSugerida("401", BigDecimal.ZERO, new BigDecimal("1000"))), 0.92, "Venta de contado");

    private ServicioPolizas servicio(PolizaSugerida sugerencia) {
        ContextoContable contexto = new ContextoContable() {
            @Override
            public List<CuentaCatalogo> catalogo() {
                return CATALOGO;
            }

            @Override
            public List<PolizaEjemplo> ejemplosRecientes() {
                return List.of();
            }
        };
        SugeridorPolizas sugeridor = (usuario, descripcion, catalogo, ejemplos) -> sugerencia;
        PresupuestoTokens presupuesto = new PresupuestoTokens(new ConsumoTokens() {
            @Override
            public long desde(String usuario, Instant desde) {
                return 0;
            }

            @Override
            public long delServicio(Instant desde) {
                return 0;
            }
        }, 150_000, 15_000, 1_000_000, RELOJ, new SimpleMeterRegistry());
        return new ServicioPolizas(contexto, sugeridor, acciones, presupuesto, RELOJ);
    }

    @Test
    void sugerirDejaUnaAccionPendienteYNoRegistraNada() {
        AccionPendiente accion = servicio(VALIDA).preparar("contador1", "Venta de contado de 1000");

        assertThat(accion.estatus()).isEqualTo(AccionPendiente.PENDIENTE);
        assertThat(accion.tipo()).isEqualTo(AccionesPendientes.TIPO_POLIZA_REGISTRAR);
        assertThat(ejecuciones).as("ninguna póliza se registra antes de confirmar").isEmpty();
    }

    @Test
    void laPolizaSeRegistraSoloCuandoElContadorConfirma() {
        AccionPendiente accion = servicio(VALIDA).preparar("contador1", "Venta de contado de 1000");
        assertThat(ejecuciones).isEmpty();

        acciones.confirmar(accion.id(), "contador1");

        assertThat(ejecuciones).hasSize(1);
        assertThat(ejecuciones.get(0)).startsWith(AccionesPendientes.TIPO_POLIZA_REGISTRAR).contains(CAJA.toString());
    }

    @Test
    void soloElQueSugirioPuedeConfirmar() {
        AccionPendiente accion = servicio(VALIDA).preparar("contador1", "Venta de contado de 1000");

        assertThatThrownBy(() -> acciones.confirmar(accion.id(), "otro-usuario"))
                .isInstanceOf(AccionNoEncontradaException.class);
        assertThat(ejecuciones).isEmpty();
    }

    @Test
    void unaSugerenciaQueNoCuadraSeRechazaSinGuardarAccion() {
        PolizaSugerida descuadrada = new PolizaSugerida("INGRESO", List.of(
                new LineaSugerida("101", new BigDecimal("1000"), BigDecimal.ZERO),
                new LineaSugerida("401", BigDecimal.ZERO, new BigDecimal("900"))), 0.9, "");

        assertThatThrownBy(() -> servicio(descuadrada).preparar("contador1", "venta"))
                .isInstanceOf(SugerenciaPolizaInvalidaException.class)
                .hasMessageContaining("no cuadra");
        assertThat(almacenadas).isEmpty();
        assertThat(ejecuciones).isEmpty();
    }

    @Test
    void unaCuentaFueraDelCatalogoSeRechaza() {
        PolizaSugerida inventada = new PolizaSugerida("INGRESO", List.of(
                new LineaSugerida("999", new BigDecimal("1000"), BigDecimal.ZERO),
                new LineaSugerida("401", BigDecimal.ZERO, new BigDecimal("1000"))), 0.9, "");

        assertThatThrownBy(() -> servicio(inventada).preparar("contador1", "venta"))
                .isInstanceOf(SugerenciaPolizaInvalidaException.class)
                .hasMessageContaining("999");
        assertThat(almacenadas).isEmpty();
    }

    @Test
    void unaLineaConCargoYAbonoALaVezSeRechaza() {
        PolizaSugerida ambos = new PolizaSugerida("DIARIO", List.of(
                new LineaSugerida("101", new BigDecimal("1000"), new BigDecimal("1000")),
                new LineaSugerida("401", BigDecimal.ZERO, new BigDecimal("0"))), 0.9, "");

        assertThatThrownBy(() -> servicio(ambos).preparar("contador1", "venta"))
                .isInstanceOf(SugerenciaPolizaInvalidaException.class);
        assertThat(almacenadas).isEmpty();
    }

    @Test
    void unTipoDePolizaDesconocidoSeRechaza() {
        PolizaSugerida tipoRaro = new PolizaSugerida("CHEQUE", VALIDA.lineas(), 0.9, "");

        assertThatThrownBy(() -> servicio(tipoRaro).preparar("contador1", "venta"))
                .isInstanceOf(SugerenciaPolizaInvalidaException.class);
        assertThat(almacenadas).isEmpty();
    }
}
