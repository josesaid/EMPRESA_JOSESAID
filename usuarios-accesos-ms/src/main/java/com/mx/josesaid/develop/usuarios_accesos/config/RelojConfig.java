package com.mx.josesaid.develop.usuarios_accesos.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj en UTC. Inyectarlo permite probar las reglas de tiempo sin depender de la hora real. */
@Configuration
public class RelojConfig {

    /** Reloj del sistema en UTC, usado para todas las fechas de usuarios y sesiones. */
    @Bean
    Clock reloj() {
        return Clock.systemUTC();
    }
}
