package com.mx.josesaid.develop.clientes.infrastructure.persistence;

import com.mx.josesaid.develop.clientes.domain.Interaccion;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InteraccionRepository extends JpaRepository<Interaccion, UUID> {

    Page<Interaccion> findByClienteId(UUID clienteId, Pageable pageable);

    Optional<Interaccion> findFirstByClienteIdOrderByFechaDesc(UUID clienteId);
}
