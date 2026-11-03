package com.mx.josesaid.develop.ui.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientesConfig {

    @Bean
    public RestClient gatewayClient(@Value("${ui.gateway-url}") String gatewayUrl) {
        return RestClient.builder().baseUrl(gatewayUrl).build();
    }
}
