package com.mx.josesaid.develop.nexus.comun.tokens;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Registra tokens consumidos y costo estimado con Micrometer.
 * Métricas:
 * <ul>
 *   <li>nexus.tokens (counter) con etiquetas servicio, usuario y direccion=entrada|salida</li>
 *   <li>nexus.costo.estimado (counter) con etiquetas servicio y usuario</li>
 * </ul>
 */
public final class RegistroTokens {

    public static final String METRICA_TOKENS = "nexus.tokens";
    public static final String METRICA_COSTO = "nexus.costo.estimado";

    private final MeterRegistry registro;
    private final PreciosTokens precios;

    /** Recibe el registro de métricas de Micrometer y la tabla de precios. Si no se pasa tabla, el costo queda en cero. */
    public RegistroTokens(MeterRegistry registro, PreciosTokens precios) {
        this.registro = registro;
        this.precios = precios == null ? PreciosTokens.SIN_PRECIO : precios;
    }

    /** Suma los tokens de entrada y salida, y el costo estimado, a las métricas del servicio y usuario indicados. Los valores negativos se tratan como cero y las etiquetas vacías como "desconocido". */
    public void registrar(String servicio, String usuario, long tokensEntrada, long tokensSalida) {
        String svc = etiqueta(servicio);
        String usr = etiqueta(usuario);
        if (tokensEntrada > 0) {
            contador(METRICA_TOKENS, svc, usr, "direccion", "entrada").increment(tokensEntrada);
        }
        if (tokensSalida > 0) {
            contador(METRICA_TOKENS, svc, usr, "direccion", "salida").increment(tokensSalida);
        }
        double costo = precios.costo(Math.max(0, tokensEntrada), Math.max(0, tokensSalida));
        if (costo > 0) {
            Counter.builder(METRICA_COSTO).tags("servicio", svc, "usuario", usr)
                    .register(registro).increment(costo);
        }
    }

    /** Obtiene o crea el contador de una métrica con sus etiquetas. Micrometer reutiliza el mismo contador para las mismas etiquetas. */
    private Counter contador(String nombre, String servicio, String usuario, String clave, String valor) {
        return Counter.builder(nombre).tags("servicio", servicio, "usuario", usuario, clave, valor)
                .register(registro);
    }

    /** Convierte un valor vacío en "desconocido" para no dejar etiquetas en blanco en las métricas. */
    private static String etiqueta(String valor) {
        return valor == null || valor.isBlank() ? "desconocido" : valor;
    }
}
