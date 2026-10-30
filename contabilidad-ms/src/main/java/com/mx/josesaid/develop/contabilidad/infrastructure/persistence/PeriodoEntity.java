package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import com.mx.josesaid.develop.contabilidad.domain.EstatusPeriodo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "periodos")
class PeriodoEntity {

    @Id
    @Column(name = "anio_mes", length = 7)
    String anioMes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    EstatusPeriodo estatus;

    @Version
    Long version;

    protected PeriodoEntity() {
    }
}
