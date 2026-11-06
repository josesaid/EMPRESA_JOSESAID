package com.mx.josesaid.develop.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class LimitadorIntentosRedisTest {

    @Test
    void permiteHastaElMaximoYBloqueaDespues() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.expire(anyString(), any())).thenReturn(Mono.just(true));
        when(ops.increment(anyString())).thenReturn(Mono.just(1L), Mono.just(2L), Mono.just(3L));

        Clock reloj = Clock.fixed(Instant.parse("2026-11-05T21:00:30Z"), ZoneOffset.UTC);
        var limitador = new LimitadorIntentosRedis(redis, 2, Duration.ofMinutes(1), reloj);

        StepVerifier.create(limitador.intentar("203.0.113.7"))
                .assertNext(r -> assertThat(r.permitido()).isTrue())
                .verifyComplete();
        StepVerifier.create(limitador.intentar("203.0.113.7"))
                .assertNext(r -> assertThat(r.permitido()).isTrue())
                .verifyComplete();
        StepVerifier.create(limitador.intentar("203.0.113.7"))
                .assertNext(r -> {
                    assertThat(r.permitido()).isFalse();
                    assertThat(r.reintentarEnSegundos()).isEqualTo(30);
                })
                .verifyComplete();
    }

    @Test
    void primerIntentoFijaExpiracion() {
        var redis = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        when(redis.expire(anyString(), any())).thenReturn(Mono.just(true));
        when(ops.increment(anyString())).thenReturn(Mono.just(1L));

        var limitador = new LimitadorIntentosRedis(redis, 5, Duration.ofMinutes(1),
                Clock.fixed(Instant.parse("2026-11-05T21:00:00Z"), ZoneOffset.UTC));
        StepVerifier.create(limitador.intentar("198.51.100.9"))
                .assertNext(r -> assertThat(r.permitido()).isTrue())
                .verifyComplete();
        org.mockito.Mockito.verify(redis).expire(anyString(), any());
    }
}
