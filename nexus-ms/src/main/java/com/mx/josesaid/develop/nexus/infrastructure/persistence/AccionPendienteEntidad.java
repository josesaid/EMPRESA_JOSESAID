package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

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

    protected AccionPendienteEntidad() {
    }

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

    public UUID getId() { return id; }
    public String getUsuario() { return usuario; }
    public String getTipo() { return tipo; }
    public String getPayload() { return payload; }
    public String getResumen() { return resumen; }
    public String getEstatus() { return estatus; }
    public Instant getCreadaUtc() { return creadaUtc; }
    public Instant getExpiraUtc() { return expiraUtc; }
}
