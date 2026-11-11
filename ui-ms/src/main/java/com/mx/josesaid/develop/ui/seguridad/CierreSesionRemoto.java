package com.mx.josesaid.develop.ui.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** Revoca la sesión en usuarios-accesos-ms al cerrar sesión en la interfaz. Es de mejor esfuerzo. */
@Component
public class CierreSesionRemoto implements LogoutHandler {

    private static final Logger log = LoggerFactory.getLogger(CierreSesionRemoto.class);

    private final RestClient gateway;

    /** Recibe el cliente del gateway para avisarle del cierre de sesión. */
    public CierreSesionRemoto(RestClient gatewayClient) {
        this.gateway = gatewayClient;
    }

    /** Al cerrar sesión en la interfaz, pide al gateway revocar la sesión con el access token. Si la llamada falla, el cierre local sigue. */
    @Override
    public void logout(HttpServletRequest peticion, HttpServletResponse respuesta, Authentication auth) {
        if (auth == null || !(auth.getDetails() instanceof SesionTokens sesion)) {
            return;
        }
        try {
            gateway.post()
                    .uri("/usuarios-accesos-ms/v1/auth/logout")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("refreshToken", sesion.getRefreshToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("No se pudo cerrar la sesión en usuarios-accesos; la sesión local sí se cierra");
        }
    }
}
