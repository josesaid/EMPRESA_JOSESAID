package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ArticuloJpaRepository extends JpaRepository<ArticuloEntity, UUID> {
    boolean existsBySku(String sku);
}
