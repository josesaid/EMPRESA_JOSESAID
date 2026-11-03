package com.mx.josesaid.develop.ui.seguridad;

import java.io.Serializable;
import java.time.Instant;

/** Tokens del usuario guardados en la sesión del servidor. El navegador nunca los recibe. */
public class SesionTokens implements Serializable {

    private String accessToken;
    private String refreshToken;
    private Instant expiraEn;

    public SesionTokens(String accessToken, String refreshToken, Instant expiraEn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiraEn = expiraEn;
    }

    public synchronized String getAccessToken() { return accessToken; }
    public synchronized String getRefreshToken() { return refreshToken; }
    public synchronized Instant getExpiraEn() { return expiraEn; }

    public synchronized void actualizar(String accessToken, String refreshToken, Instant expiraEn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiraEn = expiraEn;
    }
}
