package com.mx.josesaid.develop.nexus.application;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Datos de un alta de usuario que Nexus prepara. No contiene contraseña: la escribe el administrador
 * en la pantalla de altas, que la envía directo a usuarios-accesos.
 */
public record DatosAlta(String correo, String nombreCompleto, List<String> roles) {

    private static final ObjectMapper JSON = new ObjectMapper();

    public static DatosAlta de(String payload) {
        try {
            return JSON.readValue(payload, DatosAlta.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("El alta guardada no se puede leer", e);
        }
    }
}
