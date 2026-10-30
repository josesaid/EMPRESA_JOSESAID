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
@Table(name = "prospecto")
public class Prospecto {

    @Id
    private UUID id;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "nombre_cifrado", nullable = false)
    private String nombre;

    @Column(name = "nombre_hash", nullable = false, length = 64)
    private String nombreHash;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "correo_cifrado", nullable = false)
    private String correo;

    @Column(name = "correo_hash", nullable = false, length = 64)
    private String correoHash;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "rfc_cifrado")
    private String rfc;

    @Column(name = "rfc_hash", length = 64)
    private String rfcHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "fuente", nullable = false, length = 20)
    private Fuente fuente;

    @Enumerated(EnumType.STRING)
    @Column(name = "estatus", nullable = false, length = 20)
    private EstatusProspecto estatus;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDate fechaRegistro;

    @Column(name = "fecha_conversion")
    private Instant fechaConversion;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Prospecto() {
    }

    public static Prospecto nuevo(UUID id, String nombre, String correo, String rfc, Fuente fuente,
                                  LocalDate hoy, Instant ahora) {
        Prospecto p = new Prospecto();
        p.id = id;
        p.nombre = nombre.trim();
        p.nombreHash = IndiceHash.nombre(nombre);
        p.correo = correo.trim();
        p.correoHash = IndiceHash.correo(correo);
        p.rfc = rfc;
        p.rfcHash = rfc == null ? null : IndiceHash.rfc(rfc);
        p.fuente = fuente;
        p.estatus = EstatusProspecto.NUEVO;
        p.fechaRegistro = hoy;
        p.createdAt = ahora;
        p.updatedAt = ahora;
        return p;
    }

    /** Cambia el estatus por PATCH. Lanza TransicionInvalidaException si la transición no está permitida. */
    public void cambiarEstatus(EstatusProspecto destino, Instant ahora) {
        if (estatus == destino) {
            return;
        }
        if (!estatus.puedePasarA(destino)) {
            throw new TransicionInvalidaException("Transición de prospecto no permitida: " + estatus + " -> " + destino);
        }
        estatus = destino;
        updatedAt = ahora;
    }

    /** Solo un prospecto CALIFICADO puede convertirse. */
    public void marcarConvertido(Instant ahora) {
        if (estatus != EstatusProspecto.CALIFICADO) {
            throw new TransicionInvalidaException("Solo un prospecto CALIFICADO puede convertirse");
        }
        estatus = EstatusProspecto.CONVERTIDO;
        fechaConversion = ahora;
        updatedAt = ahora;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getRfc() {
        return rfc;
    }

    public Fuente getFuente() {
        return fuente;
    }

    public EstatusProspecto getEstatus() {
        return estatus;
    }

    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    public Instant getFechaConversion() {
        return fechaConversion;
    }

    public String getCorreoHash() {
        return correoHash;
    }

    public String getNombreHash() {
        return nombreHash;
    }

    public long getVersion() {
        return version;
    }
}
