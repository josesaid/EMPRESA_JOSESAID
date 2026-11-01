package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
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

class ConfirmacionAccionesTest {

    private final Clock reloj = Clock.fixed(Instant.parse("2026-11-15T18:00:00Z"), ZoneOffset.UTC);
    private final Map<UUID, AccionPendiente> almacenadas = new HashMap<>();
    private final List<String> ejecuciones = new ArrayList<>();
    private final CodigoConfirmacion codigos = new CodigoConfirmacion(
            "secreto-de-prueba-de-32-bytes-ok!".getBytes(StandardCharsets.UTF_8));
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
        ejecuciones.add(tipo);
        return "ok";
    }, reloj);
    private final ConfirmacionAcciones confirmaciones = new ConfirmacionAcciones(acciones, codigos);

    private AccionPendiente preparada() {
        return acciones.preparar("contador1", AccionesPendientes.TIPO_POLIZA_REGISTRAR, "{}", "resumen");
    }

    @Test
    void conElCodigoCorrectoSeEjecuta() {
        AccionPendiente accion = preparada();

        String respuesta = confirmaciones.confirmarConCodigo(accion.id(), "contador1",
                codigos.generar(accion.id(), "contador1"));

        assertThat(respuesta).isEqualTo("ok");
        assertThat(ejecuciones).hasSize(1);
        assertThat(almacenadas.get(accion.id()).estatus()).isEqualTo(AccionPendiente.CONFIRMADA);
    }

    @Test
    void unCodigoIncorrectoCancelaLaAccionSinEjecutarla() {
        AccionPendiente accion = preparada();

        assertThatThrownBy(() -> confirmaciones.confirmarConCodigo(accion.id(), "contador1", "12345678"))
                .isInstanceOf(CodigoConfirmacionInvalidoException.class);

        assertThat(ejecuciones).isEmpty();
        assertThat(almacenadas.get(accion.id()).estatus()).isEqualTo(AccionPendiente.CANCELADA);
    }

    @Test
    void otroUsuarioNoPuedeConfirmarAunqueTengaElCodigo() {
        AccionPendiente accion = preparada();

        assertThatThrownBy(() -> confirmaciones.confirmarConCodigo(accion.id(), "otro",
                codigos.generar(accion.id(), "contador1")))
                .isInstanceOf(AccionNoEncontradaException.class);
        assertThat(ejecuciones).isEmpty();
    }

    @Test
    void elCodigoSeUsaUnaSolaVez() {
        AccionPendiente accion = preparada();
        String codigo = codigos.generar(accion.id(), "contador1");
        confirmaciones.confirmarConCodigo(accion.id(), "contador1", codigo);

        assertThatThrownBy(() -> confirmaciones.confirmarConCodigo(accion.id(), "contador1", codigo))
                .isInstanceOf(AccionNoPendienteException.class);
        assertThat(ejecuciones).hasSize(1);
    }
}
