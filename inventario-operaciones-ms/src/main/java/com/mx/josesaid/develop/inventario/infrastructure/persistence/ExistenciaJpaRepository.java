package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExistenciaJpaRepository extends JpaRepository<ExistenciaEntity, UUID> {
    Optional<ExistenciaEntity> findByArticuloIdAndAlmacenId(UUID articuloId, UUID almacenId);
    List<ExistenciaEntity> findByArticuloId(UUID articuloId);
}
