package com.mx.josesaid.develop.compras.infrastructure.persistence;

import com.mx.josesaid.develop.compras.domain.EstatusProveedor;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.UUID;

@Entity
@Table(name = "proveedores")
class ProveedorEntity {
    @Id UUID id;
    @Column(name = "razon_social", nullable = false, length = 200) String razonSocial;
    @Column(name = "rfc_cifrado", nullable = false, columnDefinition = "TEXT") String rfcCifrado;
    @Column(name = "rfc_indice", nullable = false, unique = true, length = 64) String rfcIndice;
    @Column(name = "correo_cifrado", nullable = false, columnDefinition = "TEXT") String correoCifrado;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12) EstatusProveedor estatus;
    @Version Long version;

    protected ProveedorEntity() {
    }
}
