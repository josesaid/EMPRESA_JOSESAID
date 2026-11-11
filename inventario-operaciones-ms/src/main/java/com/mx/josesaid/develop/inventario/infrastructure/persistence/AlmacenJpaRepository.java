package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de almacenes con Spring Data JPA. */
interface AlmacenJpaRepository extends JpaRepository<AlmacenEntity, UUID> {
}
