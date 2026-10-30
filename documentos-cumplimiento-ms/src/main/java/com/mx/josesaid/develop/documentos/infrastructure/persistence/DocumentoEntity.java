package com.mx.josesaid.develop.documentos.infrastructure.persistence;

import com.mx.josesaid.develop.documentos.domain.Documento;
import com.mx.josesaid.develop.documentos.domain.TipoDocumento;
import com.mx.josesaid.develop.documentos.domain.VersionDocumento;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "documentos")
public class DocumentoEntity {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoDocumento tipo;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 100)
    private String area;

    @Column(name = "vence_en")
    private LocalDate venceEn;

    @Column(name = "retencion_hasta", nullable = false)
    private LocalDate retencionHasta;

    @Column(nullable = false)
    private boolean eliminado;

    @Column(name = "motivo_eliminacion", length = 300)
    private String motivoEliminacion;

    @Column(name = "eliminado_utc")
    private Instant eliminadoUtc;

    @Column(name = "revision_retencion", nullable = false)
    private boolean revisionRetencion;

    @Column(name = "alerta_vencimiento_emitida", nullable = false)
    private boolean alertaVencimientoEmitida;

    @Version
    private Long version;

    @OneToMany(mappedBy = "documento", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("numero ASC")
    private List<VersionEntity> versiones = new ArrayList<>();

    protected DocumentoEntity() {
    }

    static DocumentoEntity nuevo(Documento d) {
        DocumentoEntity e = new DocumentoEntity();
        e.id = d.getId();
        e.actualizar(d);
        return e;
    }

    /** Actualiza campos y agrega solo las versiones nuevas: las existentes no se reescriben. */
    void actualizar(Documento d) {
        this.tipo = d.getTipo();
        this.titulo = d.getTitulo();
        this.area = d.getArea();
        this.venceEn = d.getVenceEn();
        this.retencionHasta = d.getRetencionHasta();
        this.eliminado = d.isEliminado();
        this.motivoEliminacion = d.getMotivoEliminacion();
        this.eliminadoUtc = d.getEliminadoUtc();
        this.revisionRetencion = d.isRevisionRetencion();
        this.alertaVencimientoEmitida = d.isAlertaVencimientoEmitida();
        Set<UUID> existentes = versiones.stream().map(VersionEntity::getId).collect(Collectors.toSet());
        for (VersionDocumento v : d.getVersiones()) {
            if (!existentes.contains(v.id())) {
                versiones.add(new VersionEntity(this, v));
            }
        }
    }

    Documento aDominio() {
        return Documento.reconstruir(id, tipo, titulo, area, venceEn, retencionHasta,
                versiones.stream().map(VersionEntity::aDominio).toList(),
                eliminado, motivoEliminacion, eliminadoUtc, revisionRetencion, alertaVencimientoEmitida);
    }
}
