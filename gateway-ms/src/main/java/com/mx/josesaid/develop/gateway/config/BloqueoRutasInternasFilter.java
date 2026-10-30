package com.mx.josesaid.develop.gateway.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Impide que desde afuera se llegue a los endpoints internos:
 * /actuator (métricas y salud detallada) y /discovery-ms (panel de Eureka).
 * Responde 404 para no revelar que la ruta existe. Prometheus y Eureka se comunican por la red interna empresa-net.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BloqueoRutasInternasFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String ruta = exchange.getRequest().getPath().value();
        if (ruta.startsWith("/actuator") || ruta.startsWith("/discovery-ms")) {
            exchange.getResponse().setStatusCode(HttpStatus.NOT_FOUND);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }
}
