package com.mx.josesaid.develop.facturacion_pagos.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ComprobanteJpaRepository extends JpaRepository<ComprobanteEntity, UUID> {

    Optional<ComprobanteEntity> findByFolio(String folio);
}
