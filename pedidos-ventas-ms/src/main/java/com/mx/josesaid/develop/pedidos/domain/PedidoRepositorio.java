package com.mx.josesaid.develop.pedidos.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Puerto para guardar y consultar pedidos, sin depender de JPA. */
public interface PedidoRepositorio {
    Pedido guardar(Pedido pedido);
    Optional<Pedido> buscarPorId(UUID id);
    List<Pedido> buscarFiltrados(UUID clienteId, EstatusPedido estatus, Canal canal);
    List<Pedido> buscarConfirmadosEntre(Instant desde, Instant hasta);
}
