package com.mx.josesaid.develop.inventario.infrastructure.compras;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.inventario.application.RecepcionesService;
import com.mx.josesaid.develop.inventario.application.RecepcionesService.LineaRecibida;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ListenerRecepcionesTest {

    private static final UUID ALMACEN = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID ORDEN = UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    private static final UUID EVENTO = UUID.fromString("00000000-0000-0000-0000-0000000000e1");

    private final ServicioGrabado servicio = new ServicioGrabado();

    private ListenerRecepciones conAlmacen(String almacen) {
        return new ListenerRecepciones(servicio, new ObjectMapper(), almacen);
    }

    private static String recibido(String datos) {
        return "{\"tipo\":\"insumos-recibidos\",\"agregadoId\":\"" + ORDEN + "\",\"datos\":" + datos + "}";
    }

    @Test
    void registraLaRecepcionConSusLineas() {
        String datos = "{\"evento_id\":\"" + EVENTO + "\",\"lineas\":[{\"sku\":\"TOR-1\",\"cantidad\":\"5\"}]}";

        conAlmacen(ALMACEN.toString()).recibir(recibido(datos));

        assertThat(servicio.registros).hasSize(1);
        ServicioGrabado.Registro r = servicio.registros.get(0);
        assertThat(r.eventoId()).isEqualTo(EVENTO);
        assertThat(r.ordenId()).isEqualTo(ORDEN);
        assertThat(r.almacenId()).isEqualTo(ALMACEN);
        assertThat(r.lineas().get(0).sku()).isEqualTo("TOR-1");
    }

    @Test
    void ignoraEventosAjenos() {
        conAlmacen(ALMACEN.toString()).recibir("{\"tipo\":\"insumos-cotizados\",\"agregadoId\":\"" + ORDEN + "\"}");

        assertThat(servicio.registros).isEmpty();
    }

    @Test
    void descartaMensajeMalformado() {
        conAlmacen(ALMACEN.toString()).recibir("{roto");

        assertThat(servicio.registros).isEmpty();
    }

    @Test
    void descartaRecepcionSinEventoId() {
        conAlmacen(ALMACEN.toString()).recibir(recibido("{\"lineas\":[{\"sku\":\"A\",\"cantidad\":\"1\"}]}"));

        assertThat(servicio.registros).isEmpty();
    }

    @Test
    void descartaRecepcionSinDatos() {
        conAlmacen(ALMACEN.toString()).recibir("{\"tipo\":\"insumos-recibidos\",\"agregadoId\":\"" + ORDEN + "\"}");

        assertThat(servicio.registros).isEmpty();
    }

    @Test
    void descartaOrdenConIdentificadorInvalido() {
        String datos = "{\"evento_id\":\"" + EVENTO + "\",\"lineas\":[{\"sku\":\"A\",\"cantidad\":\"1\"}]}";

        conAlmacen(ALMACEN.toString()).recibir("{\"tipo\":\"insumos-recibidos\",\"agregadoId\":\"no-uuid\",\"datos\":" + datos + "}");

        assertThat(servicio.registros).isEmpty();
    }

    @Test
    void descartaRecepcionSinLineas() {
        conAlmacen(ALMACEN.toString()).recibir(recibido("{\"evento_id\":\"" + EVENTO + "\"}"));
        conAlmacen(ALMACEN.toString()).recibir(recibido("{\"evento_id\":\"" + EVENTO + "\",\"lineas\":\"no-lista\"}"));

        assertThat(servicio.registros).isEmpty();
    }

    @Test
    void noRegistraSinAlmacenConfigurado() {
        String datos = "{\"evento_id\":\"" + EVENTO + "\",\"lineas\":[{\"sku\":\"A\",\"cantidad\":\"1\"}]}";

        conAlmacen("").recibir(recibido(datos));
        conAlmacen(null).recibir(recibido(datos));

        assertThat(servicio.registros).isEmpty();
    }

    /** Servicio que registra las llamadas en lugar de tocar base de datos. */
    static final class ServicioGrabado extends RecepcionesService {
        final List<Registro> registros = new ArrayList<>();

        ServicioGrabado() {
            super(null, null, null, null, null);
        }

        @Override
        public void registrar(UUID eventoId, UUID ordenId, UUID almacenId, List<LineaRecibida> lineas) {
            registros.add(new Registro(eventoId, ordenId, almacenId, lineas));
        }

        record Registro(UUID eventoId, UUID ordenId, UUID almacenId, List<LineaRecibida> lineas) {
        }
    }
}
