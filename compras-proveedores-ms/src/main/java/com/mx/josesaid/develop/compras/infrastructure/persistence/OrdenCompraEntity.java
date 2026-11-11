package com.mx.josesaid.develop.compras.infrastructure.persistence;

import com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Fila de la tabla de órdenes de compra, con sus líneas. */
@Entity
@Table(name = "ordenes_compra")
class OrdenCompraEntity {
    @Id UUID id;
    @Column(name = "proveedor_id", nullable = false) UUID proveedorId;
    @Column(nullable = false, length = 3) String moneda;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) EstatusOrdenCompra estatus;
    @Column(name = "motivo_cancelacion", length = 200) String motivoCancelacion;
    @Column(name = "creada_por", length = 255) String creadaPor;
    @Column(name = "creado_utc", insertable = false, updatable = false) java.time.Instant creadoUtc;
    @Version Long version;
    @OneToMany(mappedBy = "orden", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    List<LineaOrdenEntity> lineas = new ArrayList<>();

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected OrdenCompraEntity() {
    }
}
