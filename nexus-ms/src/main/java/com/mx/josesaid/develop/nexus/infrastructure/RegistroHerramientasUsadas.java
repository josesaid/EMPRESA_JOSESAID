package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Colector por petición de las herramientas que el modelo ejecutó.
 * Las herramientas se invocan en el mismo hilo que la llamada al modelo, por eso se usa ThreadLocal.
 */
final class RegistroHerramientasUsadas {

    private static final ThreadLocal<List<String>> USADAS = new ThreadLocal<>();

    /** Clase de utilidad: no se instancia. */
    private RegistroHerramientasUsadas() {
    }

    /** Inicia el registro de herramientas usadas en la consulta actual. Usa un ThreadLocal, así que dura solo lo que dura la petición. */
    static void iniciar() {
        USADAS.set(new ArrayList<>());
    }

    /** Agrega el nombre de una herramienta usada a la lista de la consulta actual. Si no se inició el registro, no hace nada. */
    static void registrar(String nombre) {
        List<String> lista = USADAS.get();
        if (lista != null) {
            lista.add(nombre);
        }
    }

    /** Devuelve las herramientas usadas en la consulta y limpia el registro del hilo. */
    static List<String> terminar() {
        List<String> lista = USADAS.get();
        USADAS.remove();
        return lista == null ? List.of() : List.copyOf(lista);
    }
}
