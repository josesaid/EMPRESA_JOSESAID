package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConsultaRepositorio extends JpaRepository<ConsultaEntidad, UUID> {

    @Query("select coalesce(sum(coalesce(c.tokensEntrada, 0) + coalesce(c.tokensSalida, 0)), 0) "
            + "from ConsultaEntidad c where c.usuario = :usuario and c.creadoUtc >= :desde")
    long sumarTokens(@Param("usuario") String usuario, @Param("desde") Instant desde);
}
