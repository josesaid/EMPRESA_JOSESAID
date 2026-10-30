package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CotizacionJpaRepository extends JpaRepository<CotizacionEntity, UUID> {
}
