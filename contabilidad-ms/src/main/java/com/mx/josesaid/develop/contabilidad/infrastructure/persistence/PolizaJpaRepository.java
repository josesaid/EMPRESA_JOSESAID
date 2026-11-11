package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de pólizas con Spring Data JPA. */
interface PolizaJpaRepository extends JpaRepository<PolizaEntity, UUID> {
    List<PolizaEntity> findByAnioMes(String anioMes);
}
