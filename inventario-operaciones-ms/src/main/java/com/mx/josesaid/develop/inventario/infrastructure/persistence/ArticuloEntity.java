package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "articulos")
class ArticuloEntity {
    @Id UUID id;
    @Column(nullable = false, unique = true, length = 40) String sku;
    @Column(nullable = false, length = 150) String nombre;
    @Column(nullable = false, length = 10) String unidad;
    @Column(name = "lote_obligatorio", nullable = false) boolean loteObligatorio;
    @Column(nullable = false, precision = 14, scale = 3) BigDecimal minimo;

    protected ArticuloEntity() {
    }
}
