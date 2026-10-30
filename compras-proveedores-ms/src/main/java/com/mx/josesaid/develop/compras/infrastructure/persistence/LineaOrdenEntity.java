package com.mx.josesaid.develop.compras.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "lineas_orden")
class LineaOrdenEntity {
    @Id UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "orden_id", nullable = false) OrdenCompraEntity orden;
    @Column(name = "articulo_sku", nullable = false, length = 40) String articuloSku;
    @Column(name = "cantidad_ordenada", nullable = false, precision = 14, scale = 3) BigDecimal cantidadOrdenada;
    @Column(name = "cantidad_recibida", nullable = false, precision = 14, scale = 3) BigDecimal cantidadRecibida;
    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2) BigDecimal precioUnitario;

    protected LineaOrdenEntity() {
    }
}
