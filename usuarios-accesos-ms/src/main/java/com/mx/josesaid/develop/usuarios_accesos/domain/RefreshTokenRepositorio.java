package com.mx.josesaid.develop.usuarios_accesos.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Puerto de persistencia de refresh tokens. */
public interface RefreshTokenRepositorio {

    /** Inserta o reemplaza por id. */
    void guardar(RefreshToken token);

    Optional<RefreshToken> buscarPorHash(String hash);

    /** Revoca todos los tokens de la familia que aún no estén revocados. */
    void revocarFamilia(UUID familiaId, Instant ahora);

    /** Revoca todas las sesiones del usuario. */
    void revocarTodasDeUsuario(UUID usuarioId, Instant ahora);
}
