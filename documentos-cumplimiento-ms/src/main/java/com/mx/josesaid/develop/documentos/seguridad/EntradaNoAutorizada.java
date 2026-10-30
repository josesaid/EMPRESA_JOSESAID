package com.mx.josesaid.develop.documentos.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Responde 401 y deja el motivo en WWW-Authenticate. Solo expone el motivo de sesión cerrada;
 * cualquier otro fallo de validación se reporta como "Invalid token", sin detalles de decodificación.
 * Spring puede llamar dos veces al punto de entrada en la misma petición: el primer motivo se conserva.
 */
public class EntradaNoAutorizada implements AuthenticationEntryPoint {

    private static final String MOTIVO_GENERICO = "Invalid token";
    private static final String ATRIBUTO = EntradaNoAutorizada.class.getName() + ".motivo";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException ex)
            throws IOException {
        String previo = (String) request.getAttribute(ATRIBUTO);
        String motivo = previo != null ? previo : motivo(ex);
        request.setAttribute(ATRIBUTO, motivo);
        response.setHeader("WWW-Authenticate",
                "Bearer error=\"invalid_token\", error_description=\"" + motivo + "\"");
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }

    private static String motivo(Throwable ex) {
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t instanceof JwtValidationException jve && jve.getErrors().stream()
                    .anyMatch(e -> ValidadorRevocacion.MOTIVO.equals(e.getDescription()))) {
                return ValidadorRevocacion.MOTIVO;
            }
        }
        return MOTIVO_GENERICO;
    }
}
