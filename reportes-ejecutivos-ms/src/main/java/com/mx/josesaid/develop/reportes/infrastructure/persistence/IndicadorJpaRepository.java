package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface IndicadorJpaRepository extends JpaRepository<IndicadorEntity, UUID>, JpaSpecificationExecutor<IndicadorEntity> {

    Optional<IndicadorEntity> findByAreaAndNombreAndPeriodo(
            com.mx.josesaid.develop.reportes.domain.Area area, String nombre, String periodo);
}
