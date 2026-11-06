package com.mx.josesaid.develop.gateway.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cuenta intentos por clave en ventanas de tiempo fijas. En memoria: funciona para una instancia del gateway.
 * Con varias instancias, cada una limita por separado; para un límite global hace falta un almacén compartido (Redis).
 */
public final class LimitadorVentana {

    /** Resultado de un intento: permitido, o bloqueado con los segundos para reintentar. */
    public record Resultado(boolean permitido, long reintentarEnSegundos) {
    }

    private static final int LIMPIAR_AL_SUPERAR = 10_000;

    private record Cubo(Instant inicio, AtomicInteger conteo) {
    }

    private final int maximo;
    private final Duration ventana;
    private final Clock reloj;
    private final Map<String, Cubo> cubos = new ConcurrentHashMap<>();

    public LimitadorVentana(int maximo, Duration ventana, Clock reloj) {
        if (maximo < 1) {
            throw new IllegalArgumentException("El máximo de intentos debe ser al menos 1");
        }
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    public Resultado intentar(String clave) {
        Instant ahora = reloj.instant();
        if (cubos.size() > LIMPIAR_AL_SUPERAR) {
            cubos.entrySet().removeIf(e -> vencido(e.getValue(), ahora));
        }
        Cubo cubo = cubos.compute(clave, (k, actual) ->
                actual == null || vencido(actual, ahora) ? new Cubo(ahora, new AtomicInteger()) : actual);
        if (cubo.conteo().incrementAndGet() <= maximo) {
            return new Resultado(true, 0);
        }
        long restante = ventana.minus(Duration.between(cubo.inicio(), ahora)).toSeconds();
        return new Resultado(false, Math.max(1, restante));
    }

    private boolean vencido(Cubo cubo, Instant ahora) {
        return Duration.between(cubo.inicio(), ahora).compareTo(ventana) >= 0;
    }
}
