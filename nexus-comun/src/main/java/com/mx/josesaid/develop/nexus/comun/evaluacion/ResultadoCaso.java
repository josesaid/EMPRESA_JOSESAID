package com.mx.josesaid.develop.nexus.comun.evaluacion;

import java.util.List;

public record ResultadoCaso(String id, boolean aprobado, List<String> fallos, String respuesta) {

    public ResultadoCaso {
        fallos = List.copyOf(fallos);
    }
}
