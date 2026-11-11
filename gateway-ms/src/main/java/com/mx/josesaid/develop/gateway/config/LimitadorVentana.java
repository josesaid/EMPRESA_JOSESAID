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

    /** Contador de una ventana para una clave: inicio de la ventana y número de intentos. */
    private record Cubo(Instant inicio, AtomicInteger conteo) {
    }

    private final int maximo;
    private final Duration ventana;
    private final Clock reloj;
    private final Map<String, Cubo> cubos = new ConcurrentHashMap<>();

    /** Valida el máximo (al menos 1) y guarda la duración de la ventana. */
    public LimitadorVentana(int maximo, Duration ventana, Clock reloj) {
        if (maximo < 1) {
            throw new IllegalArgumentException("El máximo de intentos debe ser al menos 1");
        }
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    /** Registra un intento para la clave. Si la ventana venció, empieza una nueva. Cuando hay más de 10 000 claves, limpia las ventanas vencidas para no crecer sin límite. */
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

    /** Indica si la ventana de un cubo ya terminó. */
    private boolean vencido(Cubo cubo, Instant ahora) {
        return Duration.between(cubo.inicio(), ahora).compareTo(ventana) >= 0;
    }
}
