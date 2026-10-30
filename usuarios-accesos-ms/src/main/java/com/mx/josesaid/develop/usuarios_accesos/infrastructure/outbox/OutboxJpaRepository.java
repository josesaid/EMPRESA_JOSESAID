package com.mx.josesaid.develop.usuarios_accesos.infrastructure.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxJpaRepository extends JpaRepository<OutboxEntity, UUID> {

    /** Eventos pendientes, del más antiguo al más nuevo, en lotes de 100. */
    List<OutboxEntity> findTop100ByPublicadoUtcIsNullOrderByCreadoUtcAsc();
}
