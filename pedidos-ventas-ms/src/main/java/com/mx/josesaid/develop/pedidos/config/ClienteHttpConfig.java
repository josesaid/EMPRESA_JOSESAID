package com.mx.josesaid.develop.pedidos.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/**
 * Builder de RestClient para las llamadas a otros servicios. Spring Boot 4 no lo registra con el
 * starter de webmvc, así que se declara aquí.
 */
@Configuration
public class ClienteHttpConfig {

    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
