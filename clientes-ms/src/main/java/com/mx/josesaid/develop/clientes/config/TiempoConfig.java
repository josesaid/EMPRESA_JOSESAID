package com.mx.josesaid.develop.clientes.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Todo el sistema calcula el tiempo con este reloj en UTC. La conversión a zona local ocurre solo en la capa api. */
@Configuration
public class TiempoConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
