package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import com.mx.josesaid.develop.reportes.domain.Area;
import com.mx.josesaid.develop.reportes.domain.Indicador;
import com.mx.josesaid.develop.reportes.domain.Periodo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "indicadores")
public class IndicadorEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Area area;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(nullable = false, length = 10)
    private String periodo;

    @Column(nullable = false, precision = 18, scale = 4)
    private BigDecimal valor;

    @Column(name = "fuente_actualizada_utc", nullable = false)
    private Instant fuenteActualizadaUtc;

    @Version
    private Long version;

    protected IndicadorEntity() {
    }

    static IndicadorEntity nuevo(Indicador i) {
        IndicadorEntity e = new IndicadorEntity();
        e.id = i.id();
        e.actualizar(i);
        return e;
    }

    void actualizar(Indicador i) {
        this.area = i.area();
        this.nombre = i.nombre();
        this.periodo = i.periodo().clave();
        this.valor = i.valor();
        this.fuenteActualizadaUtc = i.fuenteActualizadaUtc();
    }

    Indicador aDominio() {
        return new Indicador(id, area, nombre, valor, Periodo.de(periodo), fuenteActualizadaUtc);
    }
}
