package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

class AccionesPendientesTest {

    private final Map<UUID, AccionPendiente> almacen = new HashMap<>();
    private final List<String> ejecuciones = new ArrayList<>();
    private Instant ahora;
    private AccionesPendientes servicio;
    private boolean fallarEjecucion;

    @BeforeEach
    void preparar() {
        ahora = Instant.parse("2026-11-01T00:00:00Z");
        AlmacenAcciones repo = new AlmacenAcciones() {
            @Override
            public void guardar(AccionPendiente a) {
                almacen.put(a.id(), a);
            }

            @Override
            public Optional<AccionPendiente> buscar(UUID id) {
                return Optional.ofNullable(almacen.get(id));
            }
        };
        EjecutorAcciones ejecutor = (tipo, payload) -> {
            if (fallarEjecucion) {
                throw new IllegalStateException("servicio caído");
            }
            ejecuciones.add(tipo + ":" + payload);
            return "{\"id\":\"pedido-1\"}";
        };
        Clock reloj = new Clock() {
            @Override
            public java.time.ZoneId getZone() {
                return ZoneOffset.UTC;
            }

            @Override
            public Clock withZone(java.time.ZoneId zona) {
                return this;
            }

            @Override
            public Instant instant() {
                return ahora;
            }
        };
        servicio = new AccionesPendientes(repo, ejecutor, reloj);
    }

    @Test
    void elDuenoConfirmaYSeEjecutaUnaVez() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR,
                "{\"clienteId\":\"c1\"}", "resumen");

        String respuesta = servicio.confirmar(accion.id(), "u-ventas");

        assertThat(respuesta).contains("pedido-1");
        assertThat(ejecuciones).containsExactly("PEDIDO_BORRADOR:{\"clienteId\":\"c1\"}");
        assertThat(almacen.get(accion.id()).estatus()).isEqualTo(AccionPendiente.CONFIRMADA);
    }

    @Test
    void otroUsuarioNoPuedeConfirmarLaAccion() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");

        assertThatThrownBy(() -> servicio.confirmar(accion.id(), "u-otro"))
                .isInstanceOf(AccionNoEncontradaException.class);
        assertThat(ejecuciones).isEmpty();
    }

    @Test
    void unaAccionYaConfirmadaNoSeEjecutaDeNuevo() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");
        servicio.confirmar(accion.id(), "u-ventas");

        assertThatThrownBy(() -> servicio.confirmar(accion.id(), "u-ventas"))
                .isInstanceOf(AccionNoPendienteException.class);
        assertThat(ejecuciones).hasSize(1);
    }

    @Test
    void unaAccionVencidaNoSeEjecuta() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");
        ahora = ahora.plus(AccionesPendientes.VIGENCIA).plusSeconds(1);

        assertThatThrownBy(() -> servicio.confirmar(accion.id(), "u-ventas"))
                .isInstanceOf(AccionNoPendienteException.class);
        assertThat(ejecuciones).isEmpty();
        assertThat(almacen.get(accion.id()).estatus()).isEqualTo(AccionPendiente.EXPIRADA);
    }

    @Test
    void siFallaLaEjecucionLaAccionSiguePendiente() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");
        fallarEjecucion = true;

        assertThatThrownBy(() -> servicio.confirmar(accion.id(), "u-ventas"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(almacen.get(accion.id()).estatus()).isEqualTo(AccionPendiente.PENDIENTE);

        fallarEjecucion = false;
        servicio.confirmar(accion.id(), "u-ventas");
        assertThat(ejecuciones).hasSize(1);
    }

    @Test
    void consultarNoRevelaAccionesAjenas() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");

        assertThatThrownBy(() -> servicio.consultar(accion.id(), "u-otro"))
                .isInstanceOf(AccionNoEncontradaException.class);
    }

    @Test
    void consultarMarcaComoExpiradaUnaAccionVencida() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");
        ahora = ahora.plus(AccionesPendientes.VIGENCIA).plusSeconds(1);

        AccionPendiente consultada = servicio.consultar(accion.id(), "u-ventas");

        assertThat(consultada.estatus()).isEqualTo(AccionPendiente.EXPIRADA);
        assertThat(almacen.get(accion.id()).estatus()).isEqualTo(AccionPendiente.EXPIRADA);
    }

    @Test
    void cancelarImpideConfirmarDespues() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");

        servicio.cancelar(accion.id(), "u-ventas");

        assertThat(almacen.get(accion.id()).estatus()).isEqualTo(AccionPendiente.CANCELADA);
        assertThatThrownBy(() -> servicio.confirmar(accion.id(), "u-ventas"))
                .isInstanceOf(AccionNoPendienteException.class);
        assertThat(ejecuciones).isEmpty();
    }

    @Test
    void noSePuedeCancelarUnaAccionYaConfirmada() {
        AccionPendiente accion = servicio.preparar("u-ventas", AccionesPendientes.TIPO_PEDIDO_BORRADOR, "{}", "r");
        servicio.confirmar(accion.id(), "u-ventas");

        assertThatThrownBy(() -> servicio.cancelar(accion.id(), "u-ventas"))
                .isInstanceOf(AccionNoPendienteException.class);
    }
}
