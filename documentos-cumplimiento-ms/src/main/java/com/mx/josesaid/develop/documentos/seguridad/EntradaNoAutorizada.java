package com.mx.josesaid.develop.documentos.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Responde 401 y deja el motivo en WWW-Authenticate. Solo expone el motivo de sesión cerrada;
 * cualquier otro fallo de validación se reporta como "Invalid token", sin detalles de decodificación.
 */
public class EntradaNoAutorizada implements AuthenticationEntryPoint {

    private static final String MOTIVO_GENERICO = "Invalid token";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        String motivo = MOTIVO_GENERICO;
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getMessage() != null && t.getMessage().contains(ValidadorRevocacion.MOTIVO)) {
                motivo = ValidadorRevocacion.MOTIVO;
                break;
            }
        }
        response.setHeader("WWW-Authenticate",
                "Bearer error=\"invalid_token\", error_description=\"" + motivo + "\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}
