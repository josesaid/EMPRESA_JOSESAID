package com.mx.josesaid.develop.ui.seguridad;

public record RespuestaLogin(String accessToken, String tokenType, long expiresIn, String refreshToken) {
}
