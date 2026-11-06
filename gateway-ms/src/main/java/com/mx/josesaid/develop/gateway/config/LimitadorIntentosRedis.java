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

    public LimitadorIntentosRedis(ReactiveStringRedisTemplate redis, int maximo, Duration ventana, Clock reloj) {
        this.redis = redis;
        this.maximo = maximo;
        this.ventana = ventana;
        this.reloj = reloj;
    }

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
