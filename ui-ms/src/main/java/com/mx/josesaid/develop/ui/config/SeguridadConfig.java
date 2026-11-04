package com.mx.josesaid.develop.ui.config;

import com.mx.josesaid.develop.ui.seguridad.BloqueoLoginException;
import com.mx.josesaid.develop.ui.seguridad.CambioPasswordRequeridoException;
import com.mx.josesaid.develop.ui.seguridad.CierreSesionRemoto;
import com.mx.josesaid.develop.ui.seguridad.UsuariosAuthenticationProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain cadenaSeguridad(HttpSecurity http, UsuariosAuthenticationProvider proveedor,
                                               CierreSesionRemoto cierreRemoto) throws Exception {
        http.authenticationProvider(proveedor)
            .authorizeHttpRequests(a -> a
                .requestMatchers("/login", "/cuenta/**", "/css/**", "/actuator/health").permitAll()
                .anyRequest().authenticated())
            .formLogin(f -> f
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("correo")
                .passwordParameter("password")
                .defaultSuccessUrl("/", true)
                .failureHandler((peticion, respuesta, error) -> respuesta.sendRedirect(
                        error instanceof BloqueoLoginException ? "/login?bloqueado"
                                : error instanceof CambioPasswordRequeridoException ? "/login?cambio" : "/login?error")))
            .logout(l -> l
                .logoutUrl("/logout")
                .addLogoutHandler(cierreRemoto)
                .logoutSuccessUrl("/login?salir"));
        return http.build();
    }
}
