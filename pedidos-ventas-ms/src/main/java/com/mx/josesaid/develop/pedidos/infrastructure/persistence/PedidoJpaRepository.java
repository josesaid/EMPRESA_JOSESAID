package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import com.mx.josesaid.develop.pedidos.domain.EstatusPedido;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a la tabla de pedidos con Spring Data JPA y consultas con filtros. */
public interface PedidoJpaRepository extends JpaRepository<PedidoEntity, UUID>, JpaSpecificationExecutor<PedidoEntity> {

    /** Devuelve los pedidos con esos estatus confirmados dentro de un rango de fechas. */
    @Query("select p from PedidoEntity p where p.estatus in :estatus "
            + "and p.fechaConfirmacion >= :desde and p.fechaConfirmacion < :hasta")
    List<PedidoEntity> buscarVigentesEntre(@Param("estatus") Collection<EstatusPedido> estatus,
                                           @Param("desde") Instant desde,
                                           @Param("hasta") Instant hasta);
}
