package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import com.mx.josesaid.develop.inventario.domain.TipoMovimiento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimientos_inventario")
class MovimientoInventarioEntity {
    @Id UUID id;
    @Column(name = "articulo_id", nullable = false) UUID articuloId;
    @Column(name = "almacen_id", nullable = false) UUID almacenId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) TipoMovimiento tipo;
    @Column(nullable = false, precision = 14, scale = 3) BigDecimal cantidad;
    @Column(length = 60) String lote;
    @Column(length = 100) String referencia;
    @Column(name = "creado_utc", nullable = false) Instant creadoUtc;

    protected MovimientoInventarioEntity() {
    }
}
