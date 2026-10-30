package com.mx.josesaid.develop.usuarios_accesos.seguridad;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/** Rechaza los tokens de sesiones cerradas. Se combina con la validación estándar de firma y vencimiento. */
public class ValidadorRevocacion implements OAuth2TokenValidator<Jwt> {

    /** Motivo que se devuelve al cliente cuando la sesión fue cerrada. */
    public static final String MOTIVO = "La sesion fue cerrada";

    private static final OAuth2Error REVOCADO = new OAuth2Error("invalid_token", MOTIVO, null);

    private final RevocacionSesiones revocaciones;

    public ValidadorRevocacion(RevocacionSesiones revocaciones) {
        this.revocaciones = revocaciones;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        boolean revocado = revocaciones.revocado(jwt.getClaimAsString("sid"), jwt.getSubject(), jwt.getIssuedAt());
        return revocado ? OAuth2TokenValidatorResult.failure(REVOCADO) : OAuth2TokenValidatorResult.success();
    }
}
