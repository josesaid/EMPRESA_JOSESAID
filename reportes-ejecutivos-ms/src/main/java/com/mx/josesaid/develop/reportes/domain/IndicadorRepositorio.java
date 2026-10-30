package com.mx.josesaid.develop.reportes.domain;

import java.util.List;

public interface IndicadorRepositorio {
    /** Inserta o actualiza el indicador identificado por área, nombre y periodo. */
    Indicador guardar(Indicador indicador);
    /** Indicadores del periodo; si area es null, de todas las áreas. */
    List<Indicador> buscar(Area area, Periodo periodo);
}
