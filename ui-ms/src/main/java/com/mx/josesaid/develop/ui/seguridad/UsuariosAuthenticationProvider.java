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

/** Valida el inicio de sesión llamando al servicio de acceso. Aplica el bloqueo por intentos fallidos y reenvía la IP real del cliente para que el gateway limite por usuario y no por la IP de ui-ms. */
@Component
public class UsuariosAuthenticationProvider implements AuthenticationProvider {

    private final RestClient gateway;
    private final BloqueoLogin bloqueos;
    private final IpOrigen ipOrigen;

    /** Recibe el cliente del gateway, el control de bloqueos y el resolutor de IP. */
    public UsuariosAuthenticationProvider(RestClient gatewayClient, BloqueoLogin bloqueos, IpOrigen ipOrigen) {
        this.gateway = gatewayClient;
        this.bloqueos = bloqueos;
        this.ipOrigen = ipOrigen;
    }

    /** Autentica el correo y la contraseña. 403 indica contraseña temporal; 400 o 401 cuentan como fallo; otros errores se tratan como servicio no disponible. Al éxito, guarda los tokens en los detalles de la sesión y da al usuario los roles del JWT. */
    @Override
    public Authentication authenticate(Authentication intento) throws AuthenticationException {
        String correo = intento.getName();
        String password = String.valueOf(intento.getCredentials());
        if (bloqueos.bloqueado(correo)) {
            throw new BloqueoLoginException("Demasiados intentos. Intenta de nuevo en 5 minutos.");
        }

        RespuestaLogin respuesta;
        try {
            var solicitud = gateway.post()
                    .uri("/usuarios-accesos-ms/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON);
            String ipCliente = ipOrigen.actual();
            if (ipCliente != null) {
                // El gateway limita intentos por esta IP, no por la de ui-ms (todos los usuarios compartirían un límite).
                solicitud.header("X-Forwarded-For", ipCliente);
            }
            respuesta = solicitud
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

    /** Indica que este proveedor atiende inicios de sesión con correo y contraseña. */
    @Override
    public boolean supports(Class<?> tipo) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(tipo);
    }
}
