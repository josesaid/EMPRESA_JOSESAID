package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import com.mx.josesaid.develop.reportes.domain.Area;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Foto de un indicador dentro del reporte cerrado. Sin versión: nunca se modifica después del cierre. */
@Entity
@Table(name = "reporte_indicadores")
public class IndicadorConsolidadoEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporte_id", nullable = false, updatable = false)
    private ReporteEntity reporte;

    @Column(nullable = false)
    private int posicion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Area area;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal valor;

    @Column(name = "fuente_actualizada_utc", nullable = false)
    private Instant fuenteActualizadaUtc;

    @Column(nullable = false)
    private boolean desactualizado;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected IndicadorConsolidadoEntity() {
    }

    IndicadorConsolidadoEntity(ReporteEntity reporte, int posicion, IndicadorConsolidado c) {
        this.id = c.id();
        this.reporte = reporte;
        this.posicion = posicion;
        this.area = c.area();
        this.nombre = c.nombre();
        this.valor = c.valor();
        this.fuenteActualizadaUtc = c.fuenteActualizadaUtc();
        this.desactualizado = c.desactualizado();
    }

    /** Devuelve el identificador del indicador consolidado. */
    UUID getId() {
        return id;
    }

    /** Convierte la fila en el objeto del dominio. */
    IndicadorConsolidado aDominio() {
        return new IndicadorConsolidado(id, area, nombre, valor, fuenteActualizadaUtc, desactualizado);
    }
}
