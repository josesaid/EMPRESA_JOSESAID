package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.mx.josesaid.develop.facturacion_pagos.domain.EstatusComprobante;

/** Acceso a la tabla de comprobantes con Spring Data JPA. */
interface ComprobanteJpaRepository extends JpaRepository<ComprobanteEntity, UUID> {

    Optional<ComprobanteEntity> findByFolio(String folio);

    Page<ComprobanteEntity> findByEstatusOrderByCreadoUtcDesc(EstatusComprobante estatus, Pageable pageable);
}
