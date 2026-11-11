package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la tabla de periodos con Spring Data JPA, usando el año y mes como clave. */
interface PeriodoJpaRepository extends JpaRepository<PeriodoEntity, String> {
}
