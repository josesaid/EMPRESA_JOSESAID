package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Fila de la tabla de acciones pendientes. Guarda el payload completo para poder ejecutar la acción al confirmarla. */
@Entity
@Table(name = "acciones_pendientes")
public class AccionPendienteEntidad {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String usuario;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, length = 1000)
    private String resumen;

    @Column(nullable = false, length = 12)
    private String estatus;

    @Column(name = "creada_utc", nullable = false)
    private Instant creadaUtc;

    @Column(name = "expira_utc", nullable = false)
    private Instant expiraUtc;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected AccionPendienteEntidad() {
    }

    /** Crea una fila con todos los datos de la acción pendiente. */
    public AccionPendienteEntidad(UUID id, String usuario, String tipo, String payload, String resumen,
                                  String estatus, Instant creadaUtc, Instant expiraUtc) {
        this.id = id;
        this.usuario = usuario;
        this.tipo = tipo;
        this.payload = payload;
        this.resumen = resumen;
        this.estatus = estatus;
        this.creadaUtc = creadaUtc;
        this.expiraUtc = expiraUtc;
    }

    /** Devuelve el identificador de la acción. */
    public UUID getId() { return id; }
    /** Devuelve el usuario que preparó la acción. */
    public String getUsuario() { return usuario; }
    /** Devuelve el tipo de la acción. */
    public String getTipo() { return tipo; }
    /** Devuelve el payload JSON de la acción. */
    public String getPayload() { return payload; }
    /** Devuelve el resumen legible de la acción. */
    public String getResumen() { return resumen; }
    /** Devuelve el estatus de la acción. */
    public String getEstatus() { return estatus; }
    /** Devuelve la fecha de creación en UTC. */
    public Instant getCreadaUtc() { return creadaUtc; }
    /** Devuelve la fecha de vencimiento en UTC. */
    public Instant getExpiraUtc() { return expiraUtc; }
}
