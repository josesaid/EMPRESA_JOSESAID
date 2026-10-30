package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface OrdenJpaRepository extends JpaRepository<OrdenEntity, UUID> {
}
