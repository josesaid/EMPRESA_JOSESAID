package com.mx.josesaid.develop.inventario.infrastructure.pedidos;

import com.mx.josesaid.develop.inventario.domain.ReservasPedidoRepositorio;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Registro en base de datos de los pedidos procesados y sus reservas. */
@Repository
class ReservasPedidoJdbc implements ReservasPedidoRepositorio {

    private final JdbcTemplate jdbc;
    private final Clock reloj;

    ReservasPedidoJdbc(JdbcTemplate jdbc, Clock reloj) {
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    /** Marca el evento del pedido como procesado. Devuelve falso si ya estaba procesado. */
    @Override
    public boolean marcarProcesado(UUID pedidoId, String evento) {
        return jdbc.update(
                "INSERT INTO pedidos_procesados (pedido_id, evento, procesado_utc) VALUES (?, ?, ?) ON CONFLICT DO NOTHING",
                pedidoId, evento, Timestamp.from(reloj.instant())) == 1;
    }

    /** Guarda la reserva de un artículo para un pedido. */
    @Override
    public void guardarReserva(UUID pedidoId, UUID articuloId, UUID almacenId, BigDecimal cantidad) {
        jdbc.update("INSERT INTO reservas_pedido (pedido_id, articulo_id, almacen_id, cantidad) VALUES (?, ?, ?, ?)",
                pedidoId, articuloId, almacenId, cantidad);
    }

    /** Devuelve las reservas de un pedido. */
    @Override
    public List<Reserva> reservasDe(UUID pedidoId) {
        return jdbc.query(
                "SELECT articulo_id, almacen_id, cantidad FROM reservas_pedido WHERE pedido_id = ? ORDER BY id",
                (rs, n) -> new Reserva(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getBigDecimal(3)),
                pedidoId);
    }

    /** Elimina las reservas de un pedido ya entregado. */
    @Override
    public void eliminarReservas(UUID pedidoId) {
        jdbc.update("DELETE FROM reservas_pedido WHERE pedido_id = ?", pedidoId);
    }
}
