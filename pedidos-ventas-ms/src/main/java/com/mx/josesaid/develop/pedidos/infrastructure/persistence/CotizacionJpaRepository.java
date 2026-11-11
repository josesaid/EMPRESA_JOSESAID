package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de cotizaciones con Spring Data JPA. */
public interface CotizacionJpaRepository extends JpaRepository<CotizacionEntity, UUID> {
}
