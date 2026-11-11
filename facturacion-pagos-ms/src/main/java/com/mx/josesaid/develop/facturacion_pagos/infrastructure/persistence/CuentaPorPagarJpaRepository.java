package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de cuentas por pagar con Spring Data JPA. */
public interface CuentaPorPagarJpaRepository extends JpaRepository<CuentaPorPagarEntity, UUID> {

    Optional<CuentaPorPagarEntity> findByOrdenId(UUID ordenId);
}
