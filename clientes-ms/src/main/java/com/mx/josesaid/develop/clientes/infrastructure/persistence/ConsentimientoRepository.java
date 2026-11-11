package com.mx.josesaid.develop.clientes.infrastructure.persistence;

import com.mx.josesaid.develop.clientes.domain.Consentimiento;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de consentimientos con Spring Data JPA. */
public interface ConsentimientoRepository extends JpaRepository<Consentimiento, UUID> {
}
