package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Colector por petición de las herramientas que el modelo ejecutó.
 * Las herramientas se invocan en el mismo hilo que la llamada al modelo, por eso se usa ThreadLocal.
 */
final class RegistroHerramientasUsadas {

    private static final ThreadLocal<List<String>> USADAS = new ThreadLocal<>();

    private RegistroHerramientasUsadas() {
    }

    static void iniciar() {
        USADAS.set(new ArrayList<>());
    }

    static void registrar(String nombre) {
        List<String> lista = USADAS.get();
        if (lista != null) {
            lista.add(nombre);
        }
    }

    static List<String> terminar() {
        List<String> lista = USADAS.get();
        USADAS.remove();
        return lista == null ? List.of() : List.copyOf(lista);
    }
}
