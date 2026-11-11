package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Fila de la tabla de refresh tokens. Guarda solo el hash del token, nunca el token. */
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

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected RefreshTokenEntity() {
        // requerido por JPA
    }

    /** Crea una fila de refresh token a partir de sus datos. */
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

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el id del usuario. */
    public UUID getUsuarioId() { return usuarioId; }
    /** Devuelve el id de la familia de sesión. */
    public UUID getFamiliaId() { return familiaId; }
    /** Devuelve el hash del token. */
    public String getHash() { return hash; }
    /** Devuelve la fecha de creación en UTC. */
    public Instant getCreadoUtc() { return creadoUtc; }
    /** Devuelve la fecha de expiración en UTC. */
    public Instant getExpiraUtc() { return expiraUtc; }
    /** Devuelve la fecha en que se usó, o nula si no se ha usado. */
    public Instant getUsadoUtc() { return usadoUtc; }
    /** Devuelve la fecha de revocación, o nula si sigue vigente. */
    public Instant getRevocadoUtc() { return revocadoUtc; }
}
