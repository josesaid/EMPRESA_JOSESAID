package com.mx.josesaid.develop.contabilidad.infrastructure.persistence;

import com.mx.josesaid.develop.contabilidad.domain.Naturaleza;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Fila de la tabla de cuentas contables. */
@Entity
@Table(name = "cuentas_contables")
class CuentaEntity {

    @Id
    UUID id;

    @Column(nullable = false, unique = true, length = 20)
    String codigo;

    @Column(nullable = false, length = 150)
    String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    Naturaleza naturaleza;

    @Column(nullable = false)
    boolean activa;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected CuentaEntity() {
    }
}
