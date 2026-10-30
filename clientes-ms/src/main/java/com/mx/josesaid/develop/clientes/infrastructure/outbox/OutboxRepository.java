package com.mx.josesaid.develop.clientes.infrastructure.outbox;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxRepository extends JpaRepository<OutboxEvento, UUID> {

    List<OutboxEvento> findTop100ByEnviadoAtIsNullOrderByCreatedAtAsc();
}
