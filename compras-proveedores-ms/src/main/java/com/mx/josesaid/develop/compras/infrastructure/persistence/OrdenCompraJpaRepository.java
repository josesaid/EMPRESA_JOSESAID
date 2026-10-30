package com.mx.josesaid.develop.compras.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrdenCompraJpaRepository extends JpaRepository<OrdenCompraEntity, UUID> {
}
