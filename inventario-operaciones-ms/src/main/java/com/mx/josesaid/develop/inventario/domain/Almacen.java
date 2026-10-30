package com.mx.josesaid.develop.inventario.domain;

import java.util.UUID;

public class Almacen {

    private final UUID id;
    private final String nombre;
    private final String ubicacion;

    private Almacen(UUID id, String nombre, String ubicacion) {
        this.id = id;
        this.nombre = nombre;
        this.ubicacion = ubicacion;
    }

    public static Almacen crear(String nombre, String ubicacion) {
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre del almacén es obligatorio");
        }
        if (ubicacion == null || ubicacion.isBlank()) {
            throw new ReglaDeNegocioException("La ubicación del almacén es obligatoria");
        }
        return new Almacen(UUID.randomUUID(), nombre.trim(), ubicacion.trim());
    }

    public static Almacen reconstruir(UUID id, String nombre, String ubicacion) {
        return new Almacen(id, nombre, ubicacion);
    }

    public UUID getId() { return id; }
    public String getNombre() { return nombre; }
    public String getUbicacion() { return ubicacion; }
}
