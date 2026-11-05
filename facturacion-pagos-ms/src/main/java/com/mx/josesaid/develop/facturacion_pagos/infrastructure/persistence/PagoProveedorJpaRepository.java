package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PagoProveedorJpaRepository extends JpaRepository<PagoProveedorEntity, UUID> {

    List<PagoProveedorEntity> findAllByOrderByFechaPagoDescCreadoUtcDesc(Pageable pageable);
}
