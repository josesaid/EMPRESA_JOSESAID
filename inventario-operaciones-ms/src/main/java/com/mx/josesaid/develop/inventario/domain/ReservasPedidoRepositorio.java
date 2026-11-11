package com.mx.josesaid.develop.inventario.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Reservas creadas por los pedidos y control de eventos de pedidos ya aplicados. */
public interface ReservasPedidoRepositorio {

    /** Registra el evento como aplicado. Devuelve false si ya estaba aplicado. */
    boolean marcarProcesado(UUID pedidoId, String evento);

    void guardarReserva(UUID pedidoId, UUID articuloId, UUID almacenId, BigDecimal cantidad);

    List<Reserva> reservasDe(UUID pedidoId);

    void eliminarReservas(UUID pedidoId);

    /**
     * Reserva registrada para un pedido: artículo, almacén y cantidad.
     *
     * @param articuloId componente de tipo {@code UUID}.
     * @param almacenId componente de tipo {@code UUID}.
     * @param cantidad componente de tipo {@code BigDecimal}.
     */
    record Reserva(UUID articuloId, UUID almacenId, BigDecimal cantidad) {
    }
}
