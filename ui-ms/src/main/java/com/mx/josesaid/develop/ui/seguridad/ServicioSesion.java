package com.mx.josesaid.develop.ui.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/** Entrega un access token vigente para llamar a las APIs, renovándolo si está por expirar. */
@Service
public class ServicioSesion {

    private static final Duration MARGEN = Duration.ofSeconds(60);

    private final RestClient gateway;

    public ServicioSesion(RestClient gatewayClient) {
        this.gateway = gatewayClient;
    }

    public String accessTokenVigente() {
        SesionTokens sesion = sesionActual();
        if (Instant.now().plus(MARGEN).isAfter(sesion.getExpiraEn())) {
            renovar(sesion);
        }
        return sesion.getAccessToken();
    }

    private SesionTokens sesionActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getDetails() instanceof SesionTokens sesion)) {
            throw new SesionExpiradaException("No hay sesión activa");
        }
        return sesion;
    }

    private void renovar(SesionTokens sesion) {
        RespuestaLogin nueva;
        try {
            nueva = gateway.post()
                    .uri("/usuarios-accesos-ms/v1/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refreshToken", sesion.getRefreshToken()))
                    .retrieve()
                    .body(RespuestaLogin.class);
        } catch (RestClientException e) {
            throw new SesionExpiradaException("La sesión venció; vuelva a entrar", e);
        }
        if (nueva == null || nueva.accessToken() == null) {
            throw new SesionExpiradaException("La sesión venció; vuelva a entrar");
        }
        sesion.actualizar(nueva.accessToken(), nueva.refreshToken(),
                Instant.now().plusSeconds(nueva.expiresIn()));
        persistir();
    }

    /**
     * Spring Session no detecta cambios hechos dentro de un objeto ya guardado. Se vuelve a guardar el
     * contexto para que el refresh token rotado llegue a Redis y no se reuse en la siguiente petición.
     */
    private static void persistir() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            atributos.getRequest().getSession().setAttribute(
                    HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                    SecurityContextHolder.getContext());
        }
    }
}
