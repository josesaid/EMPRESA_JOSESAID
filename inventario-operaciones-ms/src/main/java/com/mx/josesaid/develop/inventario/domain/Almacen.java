package com.mx.josesaid.develop.inventario.domain;

import java.util.UUID;

/** Lugar físico donde se guardan los artículos. */
public class Almacen {

    private final UUID id;
    private final String nombre;
    private final String ubicacion;

    /** Constructor privado: los almacenes se crean con sus fábricas. */
    private Almacen(UUID id, String nombre, String ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
    }

    /** Crea un almacén nuevo con id generado. Valida el nombre. */
    public static Almacen crear(String nombre, String ubicacion) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre del almacén es obligatorio");
        }
        if (ubicacion == null || ubicacion.isBlank()) {
            throw new ReglaDeNegocioException("La ubicación del almacén es obligatoria");
        }
        return new Almacen(UUID.randomUUID(), nombre.trim(), ubicacion.trim());
    }

    /** Reconstruye un almacén desde la base de datos sin aplicar reglas. */
    public static Almacen reconstruir(UUID id, String nombre, String ubicacion) {
        return new Almacen(id, nombre, ubicacion);
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el nombre. */
    public String getNombre() { return nombre; }
    /** Devuelve la ubicación. */
    public String getUbicacion() { return ubicacion; }
}
