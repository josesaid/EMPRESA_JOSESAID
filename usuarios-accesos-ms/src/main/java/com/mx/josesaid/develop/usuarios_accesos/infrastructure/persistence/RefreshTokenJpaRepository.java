package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByHash(String hash);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revocadoUtc = :ahora "
            + "where t.familiaId = :familia and t.revocadoUtc is null")
    int revocarFamilia(@Param("familia") UUID familia, @Param("ahora") Instant ahora);

    @Modifying
    @Query("update RefreshTokenEntity t set t.revocadoUtc = :ahora "
            + "where t.usuarioId = :usuario and t.revocadoUtc is null")
    int revocarTodasDeUsuario(@Param("usuario") UUID usuario, @Param("ahora") Instant ahora);
}
