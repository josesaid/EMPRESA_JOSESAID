package com.mx.josesaid.develop.clientes.api;

import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Identificador del usuario autenticado, tomado del claim sub, que debe ser un UUID. */
final class Actor {

    /** Clase de utilidad; no se instancia. */
    private Actor() {
    }

    /** Obtiene el id del usuario autenticado desde el claim sub del JWT, que debe ser un UUID. */
    static UUID de(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
