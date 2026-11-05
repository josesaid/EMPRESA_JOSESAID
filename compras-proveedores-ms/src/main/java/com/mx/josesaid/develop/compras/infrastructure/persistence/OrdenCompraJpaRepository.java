package com.mx.josesaid.develop.compras.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrdenCompraJpaRepository extends JpaRepository<OrdenCompraEntity, UUID> {

    List<OrdenCompraEntity> findAllByOrderByCreadoUtcDesc(Pageable pagina);

    List<OrdenCompraEntity> findByEstatusOrderByCreadoUtcDesc(com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra estatus, Pageable pagina);
}
