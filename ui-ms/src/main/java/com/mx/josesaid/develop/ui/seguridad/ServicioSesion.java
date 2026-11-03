package com.mx.josesaid.develop.ui.seguridad;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
            throw new IllegalStateException("No hay sesión activa");
        }
        return sesion;
    }

    private void renovar(SesionTokens sesion) {
        RespuestaLogin nueva = gateway.post()
                .uri("/usuarios-accesos-ms/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("refreshToken", sesion.getRefreshToken()))
                .retrieve()
                .body(RespuestaLogin.class);
        if (nueva == null) {
            throw new IllegalStateException("No se pudo renovar la sesión");
        }
        sesion.actualizar(nueva.accessToken(), nueva.refreshToken(),
                Instant.now().plusSeconds(nueva.expiresIn()));
    }
}
