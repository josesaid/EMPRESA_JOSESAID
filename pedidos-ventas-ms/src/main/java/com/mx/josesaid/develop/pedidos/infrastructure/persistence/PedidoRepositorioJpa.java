package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import com.mx.josesaid.develop.pedidos.domain.Canal;
import com.mx.josesaid.develop.pedidos.domain.EstatusPedido;
import com.mx.josesaid.develop.pedidos.domain.Pedido;
import com.mx.josesaid.develop.pedidos.domain.PedidoRepositorio;
import jakarta.persistence.criteria.Predicate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

@Repository
public class PedidoRepositorioJpa implements PedidoRepositorio {

    private final PedidoJpaRepository jpa;

    public PedidoRepositorioJpa(PedidoJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Pedido guardar(Pedido pedido) {
        PedidoEntity e = jpa.findById(pedido.getId())
                .map(existente -> {
                    existente.actualizar(pedido);
                    return existente;
                })
                .orElseGet(() -> PedidoEntity.nuevo(pedido));
        return jpa.save(e).aDominio();
    }

    @Override
    public Optional<Pedido> buscarPorId(UUID id) {
        return jpa.findById(id).map(PedidoEntity::aDominio);
    }

    @Override
    public List<Pedido> buscarFiltrados(UUID clienteId, EstatusPedido estatus, Canal canal) {
        Specification<PedidoEntity> spec = (raiz, consulta, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (clienteId != null) {
                condiciones.add(cb.equal(raiz.get("clienteId"), clienteId));
            }
            if (estatus != null) {
                condiciones.add(cb.equal(raiz.get("estatus"), estatus));
            }
            if (canal != null) {
                condiciones.add(cb.equal(raiz.get("canal"), canal));
            }
            return cb.and(condiciones.toArray(new Predicate[0]));
        };
        return jpa.findAll(spec).stream().map(PedidoEntity::aDominio).toList();
    }

    @Override
    public List<Pedido> buscarConfirmadosEntre(Instant desde, Instant hasta) {
        return jpa.buscarVigentesEntre(List.of(EstatusPedido.CONFIRMADO, EstatusPedido.ENTREGADO), desde, hasta)
                .stream().map(PedidoEntity::aDominio).toList();
    }
}
