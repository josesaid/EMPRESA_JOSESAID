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

/** Posible cliente que aún no se ha convertido. Pasa por estatus hasta convertirse en cliente. */
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

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected Prospecto() {
    }

    /** Crea un prospecto en estatus inicial y guarda huellas de búsqueda de su correo, nombre y RFC. */
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

    /** Devuelve el identificador. */
    public UUID getId() {
        return id;
    }

    /** Devuelve el nombre. */
    public String getNombre() {
        return nombre;
    }

    /** Devuelve el correo. */
    public String getCorreo() {
        return correo;
    }

    /** Devuelve el RFC. */
    public String getRfc() {
        return rfc;
    }

    /** Devuelve la fuente por la que llegó el prospecto. */
    public Fuente getFuente() {
        return fuente;
    }

    /** Devuelve el estatus. */
    public EstatusProspecto getEstatus() {
        return estatus;
    }

    /** Devuelve la fecha de registro. */
    public LocalDate getFechaRegistro() {
        return fechaRegistro;
    }

    /** Devuelve la fecha de conversión, o nula si no se ha convertido. */
    public Instant getFechaConversion() {
        return fechaConversion;
    }

    /** Devuelve la huella de búsqueda del correo, sin el correo en claro. */
    public String getCorreoHash() {
        return correoHash;
    }

    /** Devuelve la huella de búsqueda del nombre. */
    public String getNombreHash() {
        return nombreHash;
    }

    /** Devuelve la versión para control de concurrencia optimista. */
    public long getVersion() {
        return version;
    }
}
