package com.mx.josesaid.develop.clientes.domain;

import com.mx.josesaid.develop.clientes.infrastructure.crypto.CampoCifradoConverter;
import com.mx.josesaid.develop.clientes.infrastructure.crypto.IndiceHash;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Cliente activo de la empresa. Se crea desde un prospecto convertido o con alta directa. */
@Entity
@Table(name = "cliente")
public class Cliente {

    @Id
    private UUID id;

    @Column(name = "razon_social", nullable = false, length = 250)
    private String razonSocial;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "rfc_cifrado", nullable = false)
    private String rfc;

    @Column(name = "rfc_hash", nullable = false, length = 64)
    private String rfcHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 20)
    private EstatusCliente estatus;

    @Column(name = "prospecto_id")
    private UUID prospectoId;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDate fechaAlta;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected Cliente() {
    }

    /** Crea un cliente en estatus ACTIVO. */
    public static Cliente nuevo(UUID id, String razonSocial, String rfc, UUID prospectoId,
                                LocalDate fechaAlta, Instant ahora) {
        Cliente c = new Cliente();
        c.id = id;
        c.razonSocial = razonSocial.trim();
        c.rfc = rfc;
        c.rfcHash = IndiceHash.rfc(rfc);
        c.estatus = EstatusCliente.ACTIVO;
        c.prospectoId = prospectoId;
        c.fechaAlta = fechaAlta;
        c.createdAt = ahora;
        c.updatedAt = ahora;
        return c;
    }

    /** Cambia la razón social tras validar que no esté vacía. */
    public void actualizarRazonSocial(String nuevaRazonSocial, Instant ahora) {
        this.razonSocial = nuevaRazonSocial.trim();
        this.updatedAt = ahora;
    }

    /** Cambia el estatus si la transición está permitida. */
    public void cambiarEstatus(EstatusCliente destino, Instant ahora) {
        if (estatus == destino) {
            return;
        }
        if (!estatus.puedePasarA(destino)) {
            throw new TransicionInvalidaException("Un cliente en BAJA no puede reactivarse");
        }
        estatus = destino;
        updatedAt = ahora;
    }

    /** Devuelve el identificador. */
    public UUID getId() {
        return id;
    }

    /** Devuelve la razón social. */
    public String getRazonSocial() {
        return razonSocial;
    }

    /** Devuelve el RFC. */
    public String getRfc() {
        return rfc;
    }

    /** Devuelve el estatus. */
    public EstatusCliente getEstatus() {
        return estatus;
    }

    /** Devuelve el id del prospecto del que se originó, si aplica. */
    public UUID getProspectoId() {
        return prospectoId;
    }

    /** Devuelve la fecha de alta. */
    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    /** Devuelve la versión para control de concurrencia optimista. */
    public long getVersion() {
        return version;
    }
}
