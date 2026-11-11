package com.mx.josesaid.develop.documentos.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Reloj en UTC. Inyectarlo permite probar las reglas de tiempo sin depender de la hora real. */
@Configuration
public class RelojConfig {

    /** Reloj del sistema en UTC. */
    @Bean
    Clock reloj() {
        return Clock.system(ZoneId.of("America/Mexico_City"));
    }
}
