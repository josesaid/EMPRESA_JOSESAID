package com.mx.josesaid.develop.compras.infrastructure.persistence;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ProveedorJpaRepository extends JpaRepository<ProveedorEntity, UUID> {
    boolean existsByRfcIndice(String rfcIndice);

    List<ProveedorEntity> findTop20ByRazonSocialContainingIgnoreCaseOrderByRazonSocialAsc(String texto);
}
