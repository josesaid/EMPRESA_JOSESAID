package com.mx.josesaid.develop.clientes.api;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identificador del usuario autenticado, tomado del claim sub, que debe ser un UUID. */
final class Actor {

    private Actor() {
    }

    static UUID de(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
