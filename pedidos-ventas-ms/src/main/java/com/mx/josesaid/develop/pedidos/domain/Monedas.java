package com.mx.josesaid.develop.pedidos.domain;

import java.util.Map;

/** Catálogo de monedas con sus decimales. Agregar una moneda es configuración de este mapa. */
public final class Monedas {

    private static final Map<String, Integer> DECIMALES = Map.of("MXN", 2, "USD", 2);

    private Monedas() {
    }

    public static Map<String, Integer> catalogo() {
        return DECIMALES;
    }

    public static int decimales(String moneda) {
        Integer d = DECIMALES.get(moneda);
        if (d == null) {
            throw new ReglaDeNegocioException("La moneda no está en el catálogo: " + moneda);
        }
        return d;
    }
}
