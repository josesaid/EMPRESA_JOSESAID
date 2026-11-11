package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de movimientos con Spring Data JPA. */
interface MovimientoJpaRepository extends JpaRepository<MovimientoInventarioEntity, UUID> {
    List<MovimientoInventarioEntity> findByLoteOrderByCreadoUtcAsc(String lote);

    /** Devuelve los movimientos de un artículo, del más reciente al más antiguo, con límite. */
    List<MovimientoInventarioEntity> findByArticuloIdOrderByCreadoUtcDesc(UUID articuloId,
                                                                          org.springframework.data.domain.Pageable pageable);
}
