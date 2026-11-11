package com.mx.josesaid.develop.ui.seguridad;

import java.io.Serializable;
import java.time.Instant;

/** Tokens del usuario guardados en la sesión del servidor. El navegador nunca los recibe. */
public class SesionTokens implements Serializable {

    private String accessToken;
    private String refreshToken;
    private Instant expiraEn;

    /** Guarda los tokens de la sesión y la fecha en que expira el access token. */
    public SesionTokens(String accessToken, String refreshToken, Instant expiraEn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiraEn = expiraEn;
    }

    /** Devuelve el access token actual. */
    public synchronized String getAccessToken() { return accessToken; }
    /** Devuelve el refresh token actual. */
    public synchronized String getRefreshToken() { return refreshToken; }
    /** Devuelve la fecha de expiración del access token. */
    public synchronized Instant getExpiraEn() { return expiraEn; }

    /** Reemplaza los tokens tras una renovación. Está sincronizado porque varias peticiones pueden usar la misma sesión. */
    public synchronized void actualizar(String accessToken, String refreshToken, Instant expiraEn) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiraEn = expiraEn;
    }
}
