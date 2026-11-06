package com.mx.josesaid.develop.gateway.config;

import java.time.Clock;
import java.time.Duration;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Limita por IP los intentos de inicio de sesión y de cambio de contraseña inicial.
 * Complementa el bloqueo por cuenta de usuarios-accesos-ms: frena el tanteo de muchas cuentas desde una sola IP.
 * Responde 429 con Retry-After. No confía en X-Forwarded-For: se usa la IP que llega al gateway.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LimitadorIntentosAuthFilter implements WebFilter {

    static final Set<String> RUTAS = Set.of(
            "/usuarios-accesos-ms/v1/auth/login",
            "/usuarios-accesos-ms/v1/auth/cambiar-password-inicial");

    private final LimitadorVentana limitador;

    public LimitadorIntentosAuthFilter(
            @Value("${GATEWAY_LIMITE_AUTH_POR_MINUTO:20}") int maximoPorMinuto) {
        this.limitador = new LimitadorVentana(maximoPorMinuto, Duration.ofMinutes(1), Clock.systemUTC());
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())
                || !RUTAS.contains(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        var direccion = exchange.getRequest().getRemoteAddress();
        String ip = direccion == null || direccion.getAddress() == null
                ? "desconocida" : direccion.getAddress().getHostAddress();
        LimitadorVentana.Resultado r = limitador.intentar(ip);
        if (r.permitido()) {
            return chain.filter(exchange);
        }
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(r.reintentarEnSegundos()));
        return exchange.getResponse().setComplete();
    }
}
