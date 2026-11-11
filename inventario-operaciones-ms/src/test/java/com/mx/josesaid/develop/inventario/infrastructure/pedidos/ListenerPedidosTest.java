package com.mx.josesaid.develop.inventario.infrastructure.pedidos;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.inventario.application.ReservasPedidoService;
import com.mx.josesaid.develop.inventario.application.ReservasPedidoService.LineaPedido;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ListenerPedidosTest {

    private static final UUID ALMACEN = UUID.fromString("00000000-0000-0000-0000-0000000000a1");
    private static final UUID PEDIDO = UUID.fromString("00000000-0000-0000-0000-000000000021");
    private static final String PRODUCTO = "00000000-0000-0000-0000-0000000000b1";

    private final ServicioGrabado servicio = new ServicioGrabado();

    private ListenerPedidos conAlmacen(String almacen) {
        return new ListenerPedidos(servicio, new ObjectMapper(), almacen);
    }

    private static String confirmado(String datos) {
        return "{\"tipo\":\"PedidoConfirmado\",\"agregadoId\":\"" + PEDIDO + "\",\"datos\":" + datos + "}";
    }

    @Test
    void reservaLasLineasDelPedidoConfirmado() {
        String datos = "{\"lineas\":[{\"productoId\":\"" + PRODUCTO + "\",\"cantidad\":\"3\"}]}";

        conAlmacen(ALMACEN.toString()).recibir(confirmado(datos));

        assertThat(servicio.reservas).hasSize(1);
        assertThat(servicio.reservas.get(0).pedidoId()).isEqualTo(PEDIDO);
        assertThat(servicio.reservas.get(0).almacenId()).isEqualTo(ALMACEN);
        assertThat(servicio.reservas.get(0).lineas().get(0).cantidad()).isEqualByComparingTo("3");
    }

    @Test
    void descuentaAlEntregar() {
        conAlmacen(ALMACEN.toString()).recibir(
                "{\"tipo\":\"PedidoEntregado\",\"agregadoId\":\"" + PEDIDO + "\",\"datos\":{}}");

        assertThat(servicio.entregados).containsExactly(PEDIDO);
        assertThat(servicio.reservas).isEmpty();
    }

    @Test
    void ignoraEventosAjenos() {
        conAlmacen(ALMACEN.toString()).recibir("{\"tipo\":\"PedidoCancelado\",\"agregadoId\":\"" + PEDIDO + "\"}");

        assertThat(servicio.reservas).isEmpty();
        assertThat(servicio.entregados).isEmpty();
    }

    @Test
    void descartaMensajeMalformado() {
        conAlmacen(ALMACEN.toString()).recibir("{roto");

        assertThat(servicio.reservas).isEmpty();
    }

    @Test
    void descartaIdentificadorDePedidoInvalido() {
        conAlmacen(ALMACEN.toString()).recibir(
                "{\"tipo\":\"PedidoEntregado\",\"agregadoId\":\"no-uuid\",\"datos\":{}}");

        assertThat(servicio.entregados).isEmpty();
    }

    @Test
    void descartaConfirmacionSinLineas() {
        conAlmacen(ALMACEN.toString()).recibir(confirmado("{\"sinLineas\":true}"));
        conAlmacen(ALMACEN.toString()).recibir(confirmado("{\"lineas\":\"no-lista\"}"));
        conAlmacen(ALMACEN.toString()).recibir("{\"tipo\":\"PedidoConfirmado\",\"agregadoId\":\"" + PEDIDO + "\"}");

        assertThat(servicio.reservas).isEmpty();
    }

    @Test
    void noReservaSinAlmacenConfigurado() {
        String datos = "{\"lineas\":[{\"productoId\":\"" + PRODUCTO + "\",\"cantidad\":\"1\"}]}";

        conAlmacen("").recibir(confirmado(datos));
        conAlmacen("   ").recibir(confirmado(datos));

        assertThat(servicio.reservas).isEmpty();
    }

    /** Servicio que registra las llamadas en lugar de tocar base de datos. */
    static final class ServicioGrabado extends ReservasPedidoService {
        final List<Reserva> reservas = new ArrayList<>();
        final List<UUID> entregados = new ArrayList<>();

        ServicioGrabado() {
            super(null, null, null, null, null);
        }

        @Override
        public void reservar(UUID pedidoId, UUID almacenId, List<LineaPedido> lineas) {
            reservas.add(new Reserva(pedidoId, almacenId, lineas));
        }

        @Override
        public void entregar(UUID pedidoId) {
            entregados.add(pedidoId);
        }

        record Reserva(UUID pedidoId, UUID almacenId, List<LineaPedido> lineas) {
        }
    }
}
