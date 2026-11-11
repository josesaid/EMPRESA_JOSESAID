package com.mx.josesaid.develop.gateway.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Mono;

/**
 * Límite compartido entre réplicas del gateway, en Redis. Ventana fija alineada al reloj:
 * cada clave expira al terminar su ventana.
 */
public class LimitadorIntentosRedis implements LimitadorIntentos {

    private final ReactiveStringRedisTemplate redis;
    private final int maximo;
    private final Duration ventana;
    private final Clock reloj;

    /** Crea el límite compartido en Redis. El reloj se inyecta para poder probar las ventanas. */
    public LimitadorIntentosRedis(ReactiveStringRedisTemplate redis, int maximo, Duration ventana, Clock reloj) {
        this.redis = redis;
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

    /** Incrementa el contador de la ventana actual. La clave lleva el inicio de la ventana, así cada ventana empieza en cero. Al primer intento se fija su expiración. */
    @Override
    public Mono<LimitadorVentana.Resultado> intentar(String clave) {
        long segundos = ventana.toSeconds();
        long ahora = reloj.millis() / 1000;
        long inicio = (ahora / segundos) * segundos;
        long fin = inicio + segundos;
        String llave = "gw:intentos:" + clave + ":" + inicio;
        return redis.opsForValue().increment(llave)
                .flatMap(conteo -> (conteo == 1L ? redis.expire(llave, ventana.plusSeconds(1)) : Mono.just(true))
                        .thenReturn(conteo))
                .map(conteo -> conteo <= maximo
                        ? new LimitadorVentana.Resultado(true, 0)
                        : new LimitadorVentana.Resultado(false, Math.max(1, fin - ahora)));
    }
}
