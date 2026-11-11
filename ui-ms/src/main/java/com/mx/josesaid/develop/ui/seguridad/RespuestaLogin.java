package com.mx.josesaid.develop.ui.seguridad;

/** Respuesta del servicio de acceso al iniciar sesión o renovar el token. */
public record RespuestaLogin(String accessToken, String tokenType, long expiresIn, String refreshToken) {
}
