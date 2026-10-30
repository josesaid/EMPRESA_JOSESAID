package com.mx.josesaid.develop.usuarios_accesos.infrastructure.security;

import com.mx.josesaid.develop.usuarios_accesos.config.PropiedadesJwt;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import java.time.Clock;
import java.time.Instant;
import com.mx.josesaid.develop.usuarios_accesos.config.ClavesJwt;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Emite tokens de acceso firmados con RS256. El claim {@code roles} alimenta la autorización. */
@Component
public class EmisorTokens {

    private final JwtEncoder encoder;
    private final PropiedadesJwt propiedades;
    private final Clock reloj;

    public EmisorTokens(JwtEncoder encoder, PropiedadesJwt propiedades, Clock reloj) {
        this.encoder = encoder;
        this.propiedades = propiedades;
        this.reloj = reloj;
    }

    public String emitir(Usuario usuario) {
        Instant ahora = Instant.now(reloj);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(propiedades.emisor())
                .subject(usuario.getId().toString())
                .issuedAt(ahora)
                .expiresAt(ahora.plus(propiedades.duracion()))
                .claim("roles", usuario.getRoles().stream().map(Enum::name).toList())
                .build();
        JwsHeader cabecera = JwsHeader.with(SignatureAlgorithm.RS256).keyId(ClavesJwt.KID).build();
        return encoder.encode(JwtEncoderParameters.from(cabecera, claims)).getTokenValue();
    }
}
