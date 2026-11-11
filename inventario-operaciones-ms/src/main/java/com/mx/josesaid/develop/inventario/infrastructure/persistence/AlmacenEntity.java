package com.mx.josesaid.develop.inventario.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/** Fila de la tabla de almacenes. */
@Entity
@Table(name = "almacenes")
class AlmacenEntity {
    @Id UUID id;
    @Column(nullable = false, length = 100) String nombre;
    @Column(nullable = false, length = 200) String ubicacion;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected AlmacenEntity() {
    }
}
