package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import com.mx.josesaid.develop.inventario.domain.EstatusOrden;
import com.mx.josesaid.develop.inventario.domain.TipoOrden;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "ordenes_operacion")
class OrdenEntity {
    @Id UUID id;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) TipoOrden tipo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10) EstatusOrden estatus;
    @Column(name = "cantidad_objetivo", nullable = false, precision = 14, scale = 3) BigDecimal cantidadObjetivo;
    @Column(name = "cantidad_real", precision = 14, scale = 3) BigDecimal cantidadReal;
    @Version Long version;

    protected OrdenEntity() {
    }
}
