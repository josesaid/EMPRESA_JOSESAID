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

    protected Cliente() {
    }

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

    public void actualizarRazonSocial(String nuevaRazonSocial, Instant ahora) {
        this.razonSocial = nuevaRazonSocial.trim();
        this.updatedAt = ahora;
    }

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

    public UUID getId() {
        return id;
    }

    public String getRazonSocial() {
        return razonSocial;
    }

    public String getRfc() {
        return rfc;
    }

    public EstatusCliente getEstatus() {
        return estatus;
    }

    public UUID getProspectoId() {
        return prospectoId;
    }

    public LocalDate getFechaAlta() {
        return fechaAlta;
    }

    public long getVersion() {
        return version;
    }
}
