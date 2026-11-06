package com.mx.josesaid.develop.gateway.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;

@Configuration
public class LimitadorIntentosConfig {

    @Bean
    LimitadorIntentos limitadorIntentos(
            @Value("${GATEWAY_LIMITE_ALMACEN:memoria}") String almacen,
            @Value("${GATEWAY_LIMITE_AUTH_POR_MINUTO:20}") int maximoPorMinuto,
            ReactiveStringRedisTemplate redis) {
        Duration ventana = Duration.ofMinutes(1);
        if ("redis".equalsIgnoreCase(almacen)) {
            return new LimitadorIntentosRedis(redis, maximoPorMinuto, ventana, Clock.systemUTC());
        }
        return new LimitadorIntentosMemoria(new LimitadorVentana(maximoPorMinuto, ventana, Clock.systemUTC()));
    }
}
