package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "existencias")
class ExistenciaEntity {
    @Id UUID id;
    @Column(name = "articulo_id", nullable = false) UUID articuloId;
    @Column(name = "almacen_id", nullable = false) UUID almacenId;
    @Column(nullable = false, precision = 14, scale = 3) BigDecimal cantidad;
    @Column(nullable = false, precision = 14, scale = 3) BigDecimal reservado;
    @Version Long version;

    protected ExistenciaEntity() {
    }
}
