package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

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
@Table(name = "movimientos")
class MovimientoEntity {

    @Id
    UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poliza_id", nullable = false)
    PolizaEntity poliza;

    @Column(name = "cuenta_id", nullable = false)
    UUID cuentaId;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal cargo;

    @Column(nullable = false, precision = 14, scale = 2)
    BigDecimal abono;

    protected MovimientoEntity() {
    }
}
