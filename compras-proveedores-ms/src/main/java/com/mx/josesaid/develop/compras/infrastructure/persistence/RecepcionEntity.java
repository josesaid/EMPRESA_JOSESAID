package com.mx.josesaid.develop.compras.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "recepciones")
class RecepcionEntity {
    @Id UUID id;
    @Column(name = "orden_id", nullable = false) UUID ordenId;
    @Column(name = "articulo_sku", nullable = false, length = 40) String articuloSku;
    @Column(nullable = false, precision = 14, scale = 3) BigDecimal cantidad;
    @Column(nullable = false) LocalDate fecha;

    protected RecepcionEntity() {
    }
}
