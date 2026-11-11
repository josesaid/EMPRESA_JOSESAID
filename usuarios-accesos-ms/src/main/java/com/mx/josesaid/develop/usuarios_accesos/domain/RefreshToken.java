package com.mx.josesaid.develop.usuarios_accesos.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token de una sesión. Guarda el hash del token, no el token. Es inmutable: marcar
 * como usado produce una copia.
  * @param id identificador único del registro, de tipo {@code UUID}.
  * @param usuarioId identificador del usuario, de tipo {@code UUID}.
  * @param familiaId identificador del familia, de tipo {@code UUID}.
  * @param hash hash, de tipo {@code String}.
  * @param expiraUtc expira utc, de tipo {@code Instant}.
  * @param usadoUtc usado utc, de tipo {@code Instant}.
  * @param revocadoUtc revocado utc, de tipo {@code Instant}.
 */
public record RefreshToken(UUID id, UUID usuarioId, UUID familiaId, String hash,
                           Instant expiraUtc, Instant usadoUtc, Instant revocadoUtc) {

    /** Vigente: no usado, no revocado y no vencido. */
    public boolean activo(Instant ahora) {
        return usadoUtc == null && revocadoUtc == null && ahora.isBefore(expiraUtc);
    }

    /** Devuelve una copia marcada como usada en el instante indicado. El objeto original no cambia. */
    public RefreshToken comoUsado(Instant ahora) {
        return new RefreshToken(id, usuarioId, familiaId, hash, expiraUtc, ahora, revocadoUtc);
    }
}
