package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import com.mx.josesaid.develop.contabilidad.domain.EstatusPoliza;
import com.mx.josesaid.develop.contabilidad.domain.TipoPoliza;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "polizas")
class PolizaEntity {

    @Id
    UUID id;

    @Column(name = "anio_mes", nullable = false, length = 7)
    String anioMes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    TipoPoliza tipo;

    @Column(nullable = false)
    LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    EstatusPoliza estatus;

    @Column(name = "reversa_de")
    UUID reversaDe;

    @Version
    Long version;

    @OneToMany(mappedBy = "poliza", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("id ASC")
    List<MovimientoEntity> movimientos = new ArrayList<>();

    protected PolizaEntity() {
    }
}
