package com.mx.josesaid.develop.gateway.config;

import reactor.core.publisher.Mono;

/** Límite en memoria de esta instancia. Con varias réplicas, cada una limita por separado. */
public class LimitadorIntentosMemoria implements LimitadorIntentos {

    private final LimitadorVentana ventana;

    public LimitadorIntentosMemoria(LimitadorVentana ventana) {
        this.ventana = ventana;
    }

    @Override
    public Mono<LimitadorVentana.Resultado> intentar(String clave) {
        return Mono.fromCallable(() -> ventana.intentar(clave));
    }
}
