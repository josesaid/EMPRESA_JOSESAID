package com.mx.josesaid.develop.nexus.comun.evaluacion;

import java.util.List;

/**
 * Resultado de un caso: si aprobó, qué reglas falló y la respuesta que se evaluó.
 *
 * @param id identificador único del registro, de tipo {@code String}.
 * @param aprobado aprobado, de tipo {@code boolean}.
 * @param fallos fallos, de tipo {@code List<String>}.
 * @param respuesta respuesta, de tipo {@code String}.
 */
public record ResultadoCaso(String id, boolean aprobado, List<String> fallos, String respuesta) {

    public ResultadoCaso {
        fallos = List.copyOf(fallos);
    }
}
