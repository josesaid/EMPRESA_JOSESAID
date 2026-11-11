package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de pagos a proveedores con Spring Data JPA. */
public interface PagoProveedorJpaRepository extends JpaRepository<PagoProveedorEntity, UUID> {

    List<PagoProveedorEntity> findAllByOrderByFechaPagoDescCreadoUtcDesc(Pageable pageable);
}
