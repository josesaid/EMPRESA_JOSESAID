package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReporteJpaRepository extends JpaRepository<ReporteEntity, UUID> {

    Optional<ReporteEntity> findByPeriodo(String periodo);
}
