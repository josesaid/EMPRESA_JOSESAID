package com.mx.josesaid.develop.compras.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de recepciones con Spring Data JPA. */
interface RecepcionJpaRepository extends JpaRepository<RecepcionEntity, UUID> {
}
