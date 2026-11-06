package com.mx.josesaid.develop.nexus.config;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.ssl.SslBundle;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.client.RestClient;

/**
 * Cliente hacia gateway-ms que reenvía el token del usuario que pregunta.
 * Así cada servicio aplica sus propios permisos: Nexus no tiene credenciales propias.
 */
@Configuration
public class PropagacionTokenConfig {

    /**
     * Si nexus.gateway.tls-bundle tiene un nombre, usa ese bundle SSL (CA interna) para el gateway por HTTPS.
     */
    @Bean
    RestClient gatewayClient(@Value("${nexus.gateway.url}") String url,
            @Value("${nexus.gateway.tls-bundle:}") String bundle,
            ObjectProvider<SslBundles> bundles) {
        RestClient.Builder builder = RestClient.builder();
        if (!bundle.isBlank()) {
            SslBundle ssl = bundles.getObject().getBundle(bundle);
            builder.requestFactory(ClientHttpRequestFactoryBuilder.detect()
                    .build(HttpClientSettings.ofSslBundle(ssl)));
        }
        return builder
                .baseUrl(url)
                .requestInterceptor((request, body, ejecucion) -> {
                    var autenticacion = SecurityContextHolder.getContext().getAuthentication();
                    if (autenticacion instanceof JwtAuthenticationToken jwt) {
                        request.getHeaders().setBearerAuth(jwt.getToken().getTokenValue());
                    }
                    return ejecucion.execute(request, body);
                })
                .build();
    }
}
