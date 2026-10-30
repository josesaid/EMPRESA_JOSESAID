package com.mx.josesaid.develop.usuarios_accesos.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Refresh token de una sesión. Guarda el hash del token, no el token. Es inmutable: marcar
 * como usado produce una copia.
 */
public record RefreshToken(UUID id, UUID usuarioId, UUID familiaId, String hash,
                           Instant expiraUtc, Instant usadoUtc, Instant revocadoUtc) {

    /** Vigente: no usado, no revocado y no vencido. */
    public boolean activo(Instant ahora) {
        return usadoUtc == null && revocadoUtc == null && ahora.isBefore(expiraUtc);
    }

    public RefreshToken comoUsado(Instant ahora) {
        return new RefreshToken(id, usuarioId, familiaId, hash, expiraUtc, ahora, revocadoUtc);
    }
}
