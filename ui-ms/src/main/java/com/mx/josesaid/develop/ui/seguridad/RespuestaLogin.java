package com.mx.josesaid.develop.ui.seguridad;

/**
 * Respuesta del servicio de acceso al iniciar sesión o renovar el token.
 *
 * @param accessToken access token, de tipo {@code String}.
 * @param tokenType token type, de tipo {@code String}.
 * @param expiresIn expires in, de tipo {@code long}.
 * @param refreshToken refresh token, de tipo {@code String}.
 */
public record RespuestaLogin(String accessToken, String tokenType, long expiresIn, String refreshToken) {
}
