package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface CuentaJpaRepository extends JpaRepository<CuentaEntity, UUID> {
    boolean existsByCodigo(String codigo);
}
