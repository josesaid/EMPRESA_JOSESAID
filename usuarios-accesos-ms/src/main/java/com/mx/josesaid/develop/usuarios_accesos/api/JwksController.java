package com.mx.josesaid.develop.usuarios_accesos.api;

import com.mx.josesaid.develop.usuarios_accesos.config.ClavesJwt;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publica la clave pública con la que los demás servicios y el gateway validan los tokens. */
@RestController
public class JwksController {

    private final Map<String, Object> jwks;

    public JwksController(RSAKey claveRsa) {
        this.jwks = new JWKSet(claveRsa.toPublicJWK()).toJSONObject();
    }

    @GetMapping(value = "/.well-known/jwks.json", produces = "application/json")
    public Map<String, Object> jwks() {
        return jwks;
    }
}
