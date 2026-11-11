package com.mx.josesaid.develop.inventario.application;

import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import com.mx.josesaid.develop.inventario.domain.Existencia;
import com.mx.josesaid.develop.inventario.domain.ExistenciaRepositorio;
import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.MovimientoRepositorio;
import com.mx.josesaid.develop.inventario.domain.ReservasPedidoRepositorio;
import com.mx.josesaid.develop.inventario.domain.ReservasPedidoRepositorio.Reserva;
import com.mx.josesaid.develop.inventario.domain.TipoMovimiento;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserva las piezas de un pedido al confirmarlo y las descuenta al entregarlo.
 * Si al confirmar no alcanza la existencia, se reserva lo disponible y el faltante queda en el log.
 * Cada evento se aplica una sola vez, aunque Kafka lo reentregue.
 */
@Service
public class ReservasPedidoService {

    public static final String EVENTO_CONFIRMADO = "PedidoConfirmado";
    public static final String EVENTO_ENTREGADO = "PedidoEntregado";

    private static final Logger log = LoggerFactory.getLogger(ReservasPedidoService.class);

    /**
     * Línea de un pedido: artículo y cantidad a reservar.
     *
     * @param articuloId identificador del artículo, de tipo {@code UUID}.
     * @param cantidad cantidad solicitada o registrada, de tipo {@code BigDecimal}.
     */
    public record LineaPedido(UUID articuloId, BigDecimal cantidad) {
    }

    private final ArticuloRepositorio articulos;
    private final ExistenciaRepositorio existencias;
    private final MovimientoRepositorio movimientos;
    private final ReservasPedidoRepositorio reservas;
    private final Clock reloj;

    /** Recibe los repositorios de artículos, existencias, movimientos y reservas, y el reloj. */
    public ReservasPedidoService(ArticuloRepositorio articulos, ExistenciaRepositorio existencias,
                                 MovimientoRepositorio movimientos, ReservasPedidoRepositorio reservas, Clock reloj) {
        this.articulos = articulos;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    /** Reserva las piezas de un pedido confirmado. Si no alcanza la existencia, reserva lo disponible y registra el faltante. Cada pedido se procesa una sola vez. */
    @Transactional
    public void reservar(UUID pedidoId, UUID almacenId, List<LineaPedido> lineas) {
        if (!reservas.marcarProcesado(pedidoId, EVENTO_CONFIRMADO)) {
            return;
        }
        Instant ahora = Instant.now(reloj);
        for (LineaPedido linea : lineas) {
            if (articulos.buscarPorId(linea.articuloId()).isEmpty()) {
                log.warn("Pedido {}: el artículo {} no existe en inventario; no se reserva", pedidoId, linea.articuloId());
                continue;
            }
            Existencia existencia = existencias.buscar(linea.articuloId(), almacenId)
                    .orElseGet(() -> Existencia.vacia(linea.articuloId(), almacenId));
            BigDecimal pedida = linea.cantidad();
            BigDecimal aReservar = pedida.min(existencia.disponible().max(BigDecimal.ZERO));
            if (aReservar.signum() > 0) {
                existencia.aplicar(TipoMovimiento.RESERVA, aReservar);
                existencias.guardar(existencia);
                movimientos.guardar(MovimientoInventario.crear(linea.articuloId(), almacenId, TipoMovimiento.RESERVA,
                        aReservar, null, "Pedido " + pedidoId, ahora));
                reservas.guardarReserva(pedidoId, linea.articuloId(), almacenId, aReservar);
            }
            if (aReservar.compareTo(pedida) < 0) {
                log.warn("Pedido {}: artículo {} sin existencia suficiente; pedido {}, reservado {}",
                        pedidoId, linea.articuloId(), pedida.toPlainString(), aReservar.toPlainString());
            }
        }
    }

    /** Descuenta del inventario lo reservado al entregarse el pedido y libera esas reservas. Si el pedido no tiene reservas, no hace nada. */
    @Transactional
    public void entregar(UUID pedidoId) {
        if (!reservas.marcarProcesado(pedidoId, EVENTO_ENTREGADO)) {
            return;
        }
        List<Reserva> pendientes = reservas.reservasDe(pedidoId);
        if (pendientes.isEmpty()) {
            log.info("Pedido {} entregado sin reservas registradas; no hay existencias que descontar", pedidoId);
            return;
        }
        Instant ahora = Instant.now(reloj);
        for (Reserva reserva : pendientes) {
            Existencia existencia = existencias.buscar(reserva.articuloId(), reserva.almacenId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Existencia inexistente para la reserva del pedido " + pedidoId));
            existencia.liberarReserva(reserva.cantidad());
            existencia.aplicar(TipoMovimiento.SALIDA, reserva.cantidad());
            existencias.guardar(existencia);
            movimientos.guardar(MovimientoInventario.crear(reserva.articuloId(), reserva.almacenId(),
                    TipoMovimiento.SALIDA, reserva.cantidad(), null, "Pedido " + pedidoId, ahora));
        }
        reservas.eliminarReservas(pedidoId);
    }
}
