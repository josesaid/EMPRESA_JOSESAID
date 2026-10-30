package com.mx.josesaid.develop.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Valida la firma del JWT antes de reenviar la solicitud al servicio destino.
 * El login es público; el resto exige un token válido. Cada servicio vuelve a validar el suyo.
 */
@Configuration
@EnableWebFluxSecurity
public class SeguridadGatewayConfig {

    @Bean
    SecurityWebFilterChain cadenaSeguridad(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth
                        .pathMatchers("/*/v1/auth/login", "/*/v1/auth/refresh", "/*/v1/auth/logout", "/*/v1/auth/cambiar-password-inicial").permitAll()
                        .anyExchange().authenticated())
                .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
                .build();
    }
}
