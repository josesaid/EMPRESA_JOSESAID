package com.mx.josesaid.develop.nexus.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/** Guarda fragmentos de políticas con su vector y los busca filtrando por rol antes de comparar. */
public interface AlmacenFragmentos {

    /** Reemplaza todos los fragmentos de una política de forma atómica. */
    void reemplazar(String politicaId, List<FragmentoIndexado> fragmentos);

    /**
     * Devuelve los fragmentos más parecidos a la consulta que el usuario puede ver:
     * al menos uno de sus roles debe estar en la política y la política debe estar vigente en {@code hoy}.
     */
    List<ResultadoPolitica> buscar(Set<String> roles, float[] consulta, LocalDate hoy, int limite);
}
