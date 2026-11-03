package com.mx.josesaid.develop.ui.seguridad;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/** Operaciones de cuenta que no requieren sesión, como cambiar la contraseña temporal. */
@Service
public class ServicioCuenta {

    private static final Pattern DETALLE = Pattern.compile("\"detail\"\\s*:\\s*\"([^\"]*)\"");

    private final RestClient gateway;

    public ServicioCuenta(RestClient gatewayClient) {
        this.gateway = gatewayClient;
    }

    public void cambiarPasswordInicial(String correo, String actual, String nueva) {
        try {
            gateway.post()
                    .uri("/usuarios-accesos-ms/v1/auth/cambiar-password-inicial")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("correo", correo, "passwordActual", actual, "passwordNueva", nueva))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            int estado = e.getStatusCode().value();
            if (estado == 400) {
                throw new ErrorCambioPassword("La contraseña nueva debe tener entre 12 y 100 caracteres.");
            }
            if (estado == 401 || estado == 404) {
                throw new ErrorCambioPassword("Correo o contraseña actual incorrectos.");
            }
            if (estado >= 400 && estado < 500) {
                throw new ErrorCambioPassword(detalle(e.getResponseBodyAsString()));
            }
            throw new ErrorCambioPassword("No se pudo cambiar la contraseña. Intenta más tarde.");
        }
    }

    /** Usa el mensaje de negocio que devuelve la API, o uno genérico si no lo trae. */
    private static String detalle(String cuerpo) {
        Matcher m = DETALLE.matcher(cuerpo == null ? "" : cuerpo);
        return m.find() ? m.group(1) : "No se pudo cambiar la contraseña.";
    }

    public static class ErrorCambioPassword extends RuntimeException {
        public ErrorCambioPassword(String mensaje) {
            super(mensaje);
        }
    }
}
