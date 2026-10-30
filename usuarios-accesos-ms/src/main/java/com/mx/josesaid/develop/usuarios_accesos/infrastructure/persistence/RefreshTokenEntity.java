package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_tokens")
public class RefreshTokenEntity {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(nullable = false, unique = true, length = 64)
    private String hash;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    @Column(name = "expira_utc", nullable = false)
    private Instant expiraUtc;

    @Column(name = "usado_utc")
    private Instant usadoUtc;

    @Column(name = "revocado_utc")
    private Instant revocadoUtc;

    protected RefreshTokenEntity() {
        // requerido por JPA
    }

    public RefreshTokenEntity(UUID id, UUID usuarioId, UUID familiaId, String hash, Instant creadoUtc,
                              Instant expiraUtc, Instant usadoUtc, Instant revocadoUtc) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.familiaId = familiaId;
        this.hash = hash;
        this.creadoUtc = creadoUtc;
        this.expiraUtc = expiraUtc;
        this.usadoUtc = usadoUtc;
        this.revocadoUtc = revocadoUtc;
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public UUID getFamiliaId() { return familiaId; }
    public String getHash() { return hash; }
    public Instant getCreadoUtc() { return creadoUtc; }
    public Instant getExpiraUtc() { return expiraUtc; }
    public Instant getUsadoUtc() { return usadoUtc; }
    public Instant getRevocadoUtc() { return revocadoUtc; }
}
