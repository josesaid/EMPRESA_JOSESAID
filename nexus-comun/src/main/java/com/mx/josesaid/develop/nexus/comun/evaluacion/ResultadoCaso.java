package com.mx.josesaid.develop.nexus.comun.evaluacion;

import java.util.List;

/**
 * Resultado de un caso: si aprobó, qué reglas falló y la respuesta que se evaluó.
 *
 * @param id componente de tipo {@code String}.
 * @param aprobado componente de tipo {@code boolean}.
 * @param fallos componente de tipo {@code List<String>}.
 * @param respuesta componente de tipo {@code String}.
 */
public record ResultadoCaso(String id, boolean aprobado, List<String> fallos, String respuesta) {

    public ResultadoCaso {
        fallos = List.copyOf(fallos);
    }
}
