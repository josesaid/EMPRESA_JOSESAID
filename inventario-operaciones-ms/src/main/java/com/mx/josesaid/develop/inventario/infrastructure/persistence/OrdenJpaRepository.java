package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de órdenes con Spring Data JPA. */
interface OrdenJpaRepository extends JpaRepository<OrdenEntity, UUID> {
}
