package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccionPendienteRepositorio extends JpaRepository<AccionPendienteEntidad, UUID> {
}
