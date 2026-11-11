package com.mx.josesaid.develop.usuarios_accesos.api;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publica las claves públicas con las que los demás servicios y el gateway validan los tokens. */
@RestController
public class JwksController {

    private final Map<String, Object> jwks;

    /** Recibe el conjunto de claves públicas con el que se validan los tokens. */
    public JwksController(JWKSet clavesPublicas) {
        this.jwks = clavesPublicas.toJSONObject();
    }

    /** Publica las claves públicas en formato JWKS. Los demás servicios las usan para validar tokens sin consultar a este servicio. */
    @GetMapping(value = "/.well-known/jwks.json", produces = "application/json")
    public Map<String, Object> jwks() {
        return jwks;
    }
}
