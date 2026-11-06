package com.mx.josesaid.develop.gateway.config;

import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Responde 429 con Retry-After. La IP sale de IpCliente (X-Forwarded-For solo de proxies de confianza).
 * Si el almacén (Redis) falla, deja pasar la solicitud y registra la falla: el bloqueo por cuenta sigue activo.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LimitadorIntentosAuthFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(LimitadorIntentosAuthFilter.class);

    static final Set<String> RUTAS = Set.of(
            "/usuarios-accesos-ms/v1/auth/login",
            "/usuarios-accesos-ms/v1/auth/cambiar-password-inicial");

    private final LimitadorIntentos limitador;
    private final List<String> proxiesConfiables;

    public LimitadorIntentosAuthFilter(
            LimitadorIntentos limitador,
            @Value("${GATEWAY_PROXIES_CONFIABLES:}") String proxies) {
        this.limitador = limitador;
        this.proxiesConfiables = List.of(proxies.split(","));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        if (!HttpMethod.POST.equals(exchange.getRequest().getMethod())
                || !RUTAS.contains(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }
        var direccion = exchange.getRequest().getRemoteAddress();
        String directa = direccion == null || direccion.getAddress() == null
                ? null : direccion.getAddress().getHostAddress();
        String ip = IpCliente.resolver(directa,
                exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"), proxiesConfiables);
        return limitador.intentar(ip)
                .onErrorResume(e -> {
                    log.warn("Límite de intentos no disponible; se deja pasar la solicitud: {}",
                            e.getClass().getSimpleName());
                    return Mono.just(new LimitadorVentana.Resultado(true, 0));
                })
                .flatMap(r -> {
                    if (r.permitido()) {
                        return chain.filter(exchange);
                    }
                    exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    exchange.getResponse().getHeaders().set("Retry-After", String.valueOf(r.reintentarEnSegundos()));
                    return exchange.getResponse().setComplete();
                });
    }
}
