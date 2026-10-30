package com.mx.josesaid.develop.facturacion_pagos.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj en UTC. Inyectarlo permite probar las reglas de tiempo sin depender de la hora real. */
@Configuration
public class RelojConfig {

    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
