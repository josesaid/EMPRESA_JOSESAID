package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Acceso a la tabla de indicadores con Spring Data JPA y consultas por especificación. */
public interface IndicadorJpaRepository extends JpaRepository<IndicadorEntity, UUID>, JpaSpecificationExecutor<IndicadorEntity> {

    /** Busca el indicador de un área, nombre y periodo. Lo usa la regla de que un indicador se actualiza en vez de duplicarse. */
    Optional<IndicadorEntity> findByAreaAndNombreAndPeriodo(
            com.mx.josesaid.develop.reportes.domain.Area area, String nombre, String periodo);
}
