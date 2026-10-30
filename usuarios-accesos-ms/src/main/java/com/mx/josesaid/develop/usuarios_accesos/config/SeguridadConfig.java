package com.mx.josesaid.develop.usuarios_accesos.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Seguridad del servicio: API sin estado, JWT como resource server y autorización por rol.
 *
 * <p>Solo el login y el health son públicos. Todo lo demás exige un token válido.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SeguridadConfig {

    @Bean
    SecurityFilterChain cadenaSeguridad(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API sin cookies de sesión: no aplica CSRF
            .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v1/auth/login", "/v1/auth/refresh", "/v1/auth/logout", "/actuator/health", "/actuator/health/**", "/actuator/prometheus", "/.well-known/jwks.json").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(jwt -> jwt.jwtAuthenticationConverter(convertidorJwt())));
        return http.build();
    }

    /** Convierte el claim {@code roles} en autoridades {@code ROLE_*}. */
    @Bean
    JwtAuthenticationConverter convertidorJwt() {
        JwtGrantedAuthoritiesConverter autoridades = new JwtGrantedAuthoritiesConverter();
        autoridades.setAuthoritiesClaimName("roles");
        autoridades.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter convertidor = new JwtAuthenticationConverter();
        convertidor.setJwtGrantedAuthoritiesConverter(autoridades);
        return convertidor;
    }

    /** Clave RSA (privada y pública) cargada desde usuarios.jwt.clave-privada. */
    @Bean
    RSAKey claveRsa(PropiedadesJwt propiedades) {
        return ClavesJwt.desdeBase64(propiedades.clavePrivada());
    }

    /** Valida los tokens con la clave pública; este mismo servicio también es resource server. */
    @Bean
    JwtDecoder jwtDecoder(RSAKey claveRsa) throws Exception {
        return NimbusJwtDecoder.withPublicKey(claveRsa.toRSAPublicKey()).build();
    }

    /** Firma los tokens con la clave privada (RS256). */
    @Bean
    JwtEncoder jwtEncoder(RSAKey claveRsa) {
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(claveRsa)));
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
