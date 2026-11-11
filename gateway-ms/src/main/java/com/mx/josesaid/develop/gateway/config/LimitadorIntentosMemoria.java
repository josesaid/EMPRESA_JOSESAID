package com.mx.josesaid.develop.gateway.config;

import reactor.core.publisher.Mono;

/** Límite en memoria de esta instancia. Con varias réplicas, cada una limita por separado. */
public class LimitadorIntentosMemoria implements LimitadorIntentos {

    private final LimitadorVentana ventana;

    /** Recibe la ventana de conteo en memoria que usa esta instancia. */
    public LimitadorIntentosMemoria(LimitadorVentana ventana) {
        this.ventana = ventana;
    }

    /** Registra un intento para la clave y devuelve si está permitido. Se ejecuta de forma bloqueante dentro de Mono.fromCallable. */
    @Override
    public Mono<LimitadorVentana.Resultado> intentar(String clave) {
        return Mono.fromCallable(() -> ventana.intentar(clave));
    }
}
