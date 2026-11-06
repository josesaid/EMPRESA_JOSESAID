package com.mx.josesaid.develop.gateway.config;

import reactor.core.publisher.Mono;

/** Cuenta intentos por clave (IP) y decide si se permiten. Hay una implementación en memoria y otra en Redis. */
public interface LimitadorIntentos {

    Mono<LimitadorVentana.Resultado> intentar(String clave);
}
