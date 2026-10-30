package com.mx.josesaid.develop.reportes.infrastructure.persistence;

import com.mx.josesaid.develop.reportes.domain.EstatusReporte;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import com.mx.josesaid.develop.reportes.domain.Periodo;
import com.mx.josesaid.develop.reportes.domain.Reporte;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "reportes")
public class ReporteEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 10)
    private String periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstatusReporte estatus;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    @Column(name = "cerrado_utc")
    private Instant cerradoUtc;

    @Version
    private Long version;

    @OneToMany(mappedBy = "reporte", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("posicion ASC")
    private List<IndicadorConsolidadoEntity> indicadores = new ArrayList<>();

    protected ReporteEntity() {
    }

    static ReporteEntity nuevo(Reporte r) {
        ReporteEntity e = new ReporteEntity();
        e.id = r.getId();
        e.actualizar(r);
        return e;
    }

    /** Solo agrega la foto del cierre una vez; los campos del reporte se actualizan al cerrar. */
    void actualizar(Reporte r) {
        this.periodo = r.getPeriodo().clave();
        this.estatus = r.getEstatus();
        this.creadoUtc = r.getCreadoUtc();
        this.cerradoUtc = r.getCerradoUtc();
        Set<UUID> existentes = indicadores.stream().map(IndicadorConsolidadoEntity::getId).collect(Collectors.toSet());
        List<IndicadorConsolidado> consolidados = r.getIndicadoresCerrados();
        for (int i = 0; i < consolidados.size(); i++) {
            if (!existentes.contains(consolidados.get(i).id())) {
                indicadores.add(new IndicadorConsolidadoEntity(this, i, consolidados.get(i)));
            }
        }
    }

    Reporte aDominio() {
        return Reporte.reconstruir(id, Periodo.de(periodo), creadoUtc, estatus, cerradoUtc,
                indicadores.stream().map(IndicadorConsolidadoEntity::aDominio).toList());
    }
}
