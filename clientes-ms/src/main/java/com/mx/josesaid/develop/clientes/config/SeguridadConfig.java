package com.mx.josesaid.develop.clientes.config;

import com.mx.josesaid.develop.clientes.seguridad.EntradaNoAutorizada;

import com.mx.josesaid.develop.clientes.seguridad.RevocacionSesiones;
import com.mx.josesaid.develop.clientes.seguridad.ValidadorRevocacion;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * API sin estado. Los roles salen del claim configurado en app.seguridad.claim-roles y
 * se convierten en autoridades ROLE_*. Salud, info y métricas son públicas dentro de la red interna.
 */
@Configuration
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http,
                                               @Value("${app.seguridad.claim-roles}") String claimRoles) throws Exception {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName(claimRoles);
        autoridades.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);

        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth -> oauth.authenticationEntryPoint(new EntradaNoAutorizada()).jwt(jwt -> jwt.jwtAuthenticationConverter(convertidor)))
                .build();
    }

    /** Valida los tokens por JWKS y rechaza las sesiones cerradas en usuarios-accesos-ms. */
    @Bean
    JwtDecoder jwtDecoder(@Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwksUri,
                          RevocacionSesiones revocaciones) {
        var decoder = NimbusJwtDecoder.withJwkSetUri(jwksUri).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(),
                new ValidadorRevocacion(revocaciones)));
        return decoder;
    }
}
