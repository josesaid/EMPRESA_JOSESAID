package com.mx.josesaid.develop.ui.seguridad;

/**
 * Respuesta del servicio de acceso al iniciar sesión o renovar el token.
 *
 * @param accessToken componente de tipo {@code String}.
 * @param tokenType componente de tipo {@code String}.
 * @param expiresIn componente de tipo {@code long}.
 * @param refreshToken componente de tipo {@code String}.
 */
public record RespuestaLogin(String accessToken, String tokenType, long expiresIn, String refreshToken) {
}
