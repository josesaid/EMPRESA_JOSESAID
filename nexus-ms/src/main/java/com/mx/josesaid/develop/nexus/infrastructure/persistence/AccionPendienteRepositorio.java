package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Acceso a la tabla de acciones pendientes con Spring Data JPA. */
public interface AccionPendienteRepositorio extends JpaRepository<AccionPendienteEntidad, UUID> {

    @Modifying
    @Query("delete from AccionPendienteEntidad a where a.expiraUtc < :limite")
    int borrarVencidasAntesDe(@Param("limite") Instant limite);
}
