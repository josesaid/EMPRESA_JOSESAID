package com.mx.josesaid.develop.discovery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Protege el panel y la API de Eureka con usuario y contraseña (HTTP Basic).
 * Los servicios de área se registran con esas credenciales desde su EUREKA_URL.
 * Prometheus puede leer /actuator/prometheus sin credenciales: solo expone métricas,
 * y el gateway no reenvía /actuator ni /discovery-ms hacia afuera.
 */
@Configuration
public class SeguridadEurekaConfig {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http
            // Los clientes de Eureka hacen POST sin token CSRF; la autenticación básica protege el acceso.
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info", "/actuator/prometheus").permitAll()
                .anyRequest().authenticated())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
