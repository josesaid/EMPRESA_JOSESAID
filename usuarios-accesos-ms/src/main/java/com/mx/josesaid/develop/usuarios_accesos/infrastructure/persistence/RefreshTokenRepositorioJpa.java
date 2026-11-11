package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import com.mx.josesaid.develop.usuarios_accesos.domain.RefreshToken;
import com.mx.josesaid.develop.usuarios_accesos.domain.RefreshTokenRepositorio;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/** Adaptador JPA del puerto de refresh tokens. */
@Repository
public class RefreshTokenRepositorioJpa implements RefreshTokenRepositorio {

    private final RefreshTokenJpaRepository jpa;

    /** Recibe el repositorio JPA de refresh tokens. */
    public RefreshTokenRepositorioJpa(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    /** Guarda o actualiza un refresh token. */
    @Override
    public void guardar(RefreshToken t) {
        Instant creado = jpa.findById(t.id()).map(RefreshTokenEntity::getCreadoUtc).orElseGet(Instant::now);
        jpa.save(new RefreshTokenEntity(t.id(), t.usuarioId(), t.familiaId(), t.hash(),
                creado, t.expiraUtc(), t.usadoUtc(), t.revocadoUtc()));
    }

    /** Busca un refresh token por el hash de su valor. */
    @Override
    public Optional<RefreshToken> buscarPorHash(String hash) {
        return jpa.findByHash(hash).map(e -> new RefreshToken(e.getId(), e.getUsuarioId(), e.getFamiliaId(),
                e.getHash(), e.getExpiraUtc(), e.getUsadoUtc(), e.getRevocadoUtc()));
    }

    /** Revoca todos los tokens de una familia de sesión en la fecha indicada. */
    @Override
    public void revocarFamilia(UUID familiaId, Instant ahora) {
        jpa.revocarFamilia(familiaId, ahora);
    }

    /** Revoca todas las sesiones de un usuario en la fecha indicada. */
    @Override
    public void revocarTodasDeUsuario(UUID usuarioId, Instant ahora) {
        jpa.revocarTodasDeUsuario(usuarioId, ahora);
    }
}
