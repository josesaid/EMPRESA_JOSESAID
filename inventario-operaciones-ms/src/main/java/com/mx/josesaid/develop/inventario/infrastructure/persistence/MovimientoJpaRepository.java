package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MovimientoJpaRepository extends JpaRepository<MovimientoInventarioEntity, UUID> {
    List<MovimientoInventarioEntity> findByLoteOrderByCreadoUtcAsc(String lote);
}
