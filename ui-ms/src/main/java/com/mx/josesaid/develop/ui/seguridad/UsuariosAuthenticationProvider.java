package com.mx.josesaid.develop.ui.seguridad;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class UsuariosAuthenticationProvider implements AuthenticationProvider {

    private final RestClient gateway;
    private final BloqueoLogin bloqueos;

    public UsuariosAuthenticationProvider(RestClient gatewayClient, BloqueoLogin bloqueos) {
        this.gateway = gatewayClient;
        this.bloqueos = bloqueos;
    }

    @Override
    public Authentication authenticate(Authentication intento) throws AuthenticationException {
        String correo = intento.getName();
        String password = String.valueOf(intento.getCredentials());
        if (bloqueos.bloqueado(correo)) {
            throw new BloqueoLoginException("Demasiados intentos. Intenta de nuevo en 15 minutos.");
        }

        RespuestaLogin respuesta;
        try {
            respuesta = gateway.post()
                    .uri("/usuarios-accesos-ms/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("correo", correo, "password", password))
                    .retrieve()
                    .body(RespuestaLogin.class);
        } catch (RestClientResponseException e) {
            int estado = e.getStatusCode().value();
            if (estado == 403) {
                throw new CambioPasswordRequeridoException("Debe cambiar su contraseña temporal");
            }
            if (estado == 400 || estado == 401) {
                bloqueos.fallo(correo);
                throw new BadCredentialsException("Correo o contraseña incorrectos");
            }
            throw new AuthenticationServiceException("El servicio de acceso no está disponible", e);
        }
        if (respuesta == null || respuesta.accessToken() == null) {
            throw new AuthenticationServiceException("Respuesta vacía del servicio de acceso");
        }
        bloqueos.exito(correo);

        List<GrantedAuthority> permisos = RolesJwt.extraer(respuesta.accessToken()).stream()
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();

        var autenticado = new UsernamePasswordAuthenticationToken(correo, null, permisos);
        autenticado.setDetails(new SesionTokens(
                respuesta.accessToken(),
                respuesta.refreshToken(),
                Instant.now().plusSeconds(respuesta.expiresIn())));
        return autenticado;
    }

    @Override
    public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }
}
