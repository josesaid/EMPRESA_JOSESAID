package com.mx.josesaid.develop.documentos.infrastructure.persistence;

import com.mx.josesaid.develop.documentos.domain.VersionDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "versiones_documento")
public class VersionEntity {

    @Id
    private UUID id;

    /** Dueño de la relación. updatable=false: la clave foránea se escribe solo al insertar, nunca después. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false, updatable = false)
    private DocumentoEntity documento;

    @Column(nullable = false)
    private int numero;

    @Column(name = "hash_sha256", nullable = false, length = 64)
    private String hashSha256;

    @Column(name = "almacenamiento_ref", nullable = false, length = 300)
    private String almacenamientoRef;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    protected VersionEntity() {
    }

    VersionEntity(DocumentoEntity documento, VersionDocumento v) {
        this.documento = documento;
        this.id = v.id();
        this.numero = v.numero();
        this.hashSha256 = v.hashSha256();
        this.almacenamientoRef = v.almacenamientoRef();
        this.creadoUtc = v.creadoUtc();
    }

    UUID getId() {
        return id;
    }

    VersionDocumento aDominio() {
        return new VersionDocumento(id, numero, hashSha256, almacenamientoRef, creadoUtc);
    }
}
