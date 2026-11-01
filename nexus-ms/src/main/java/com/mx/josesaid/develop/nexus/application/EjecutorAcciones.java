package com.mx.josesaid.develop.nexus.application;

/** Ejecuta una acción confirmada llamando al servicio de negocio con el token del usuario. */
public interface EjecutorAcciones {

    /** Devuelve el cuerpo de la respuesta del servicio. Lanza excepción si la operación falla. */
    String ejecutar(String tipo, String payload);
}
