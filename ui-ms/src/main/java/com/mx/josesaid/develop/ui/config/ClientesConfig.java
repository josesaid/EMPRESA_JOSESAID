package com.mx.josesaid.develop.ui.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class ClientesConfig {

    /**
     * Cliente hacia el gateway. Si GATEWAY_TLS_BUNDLE tiene un nombre, usa ese bundle SSL
     * (la CA interna) para confiar en el certificado del gateway. Si está vacío, HTTP normal.
     */
    @Bean
    public RestClient gatewayClient(@Value("${ui.gateway-url}") String gatewayUrl,
            @Value("${ui.gateway-tls-bundle:}") String bundle,
            ObjectProvider<SslBundles> bundles) {
        RestClient.Builder builder = RestClient.builder().baseUrl(gatewayUrl);
        if (!bundle.isBlank()) {
            SslBundle ssl = bundles.getObject().getBundle(bundle);
            builder.requestFactory(ClientHttpRequestFactoryBuilder.detect()
                    .build(HttpClientSettings.ofSslBundle(ssl)));
        }
        return builder.build();
    }
}
