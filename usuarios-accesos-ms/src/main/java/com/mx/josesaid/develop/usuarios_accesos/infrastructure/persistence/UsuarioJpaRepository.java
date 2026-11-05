package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Repositorio Spring Data. Solo lo usa el adaptador de persistencia. */
public interface UsuarioJpaRepository extends JpaRepository<UsuarioEntity, UUID> {

    Optional<UsuarioEntity> findByCorreoIndice(String correoIndice);

    boolean existsByCorreoIndice(String correoIndice);
}
