package com.mx.josesaid.develop.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Rechaza con 413 las solicitudes cuyo Content-Length supera el límite.
 * El límite general es de 64 KB. La carga de documentos (POST /documentos-cumplimiento-ms/v1/documentos
 * y sus versiones) tiene su propio límite, que sigue al límite de archivo del servicio (20 MB).
 * Solo revisa el encabezado Content-Length: una solicitud con transferencia chunked sin ese
 * encabezado no se corta aquí, y los servicios siguen aplicando sus propios límites.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class LimiteTamanoCuerpoFilter implements WebFilter {

    static final String PREFIJO_DOCUMENTOS = "/documentos-cumplimiento-ms/v1/documentos";

    private final long maximoGeneral;
    private final long maximoDocumentos;

    public LimiteTamanoCuerpoFilter(
            @Value("${gateway.cuerpo.maximo-bytes:65536}") long maximoGeneral,
            @Value("${gateway.cuerpo.maximo-documentos-bytes:21000000}") long maximoDocumentos) {
        this.maximoGeneral = maximoGeneral;
        this.maximoDocumentos = maximoDocumentos;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long tamano = exchange.getRequest().getHeaders().getContentLength();
        if (tamano < 0) {
            return chain.filter(exchange);
        }
        long limite = esCargaDocumentos(exchange) ? maximoDocumentos : maximoGeneral;
        if (tamano > limite) {
            exchange.getResponse().setStatusCode(HttpStatus.PAYLOAD_TOO_LARGE);
            return exchange.getResponse().setComplete();
        }
        return chain.filter(exchange);
    }

    private boolean esCargaDocumentos(ServerWebExchange exchange) {
        return HttpMethod.POST.equals(exchange.getRequest().getMethod())
                && exchange.getRequest().getPath().value().startsWith(PREFIJO_DOCUMENTOS);
    }
}
