package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Decide qué herramientas se ofrecen al modelo según los roles del usuario que pregunta.
 * Si el usuario no tiene el rol, la herramienta no se ofrece: el modelo no puede pedirla.
 */
final class AlcanceHerramientas {

    private AlcanceHerramientas() {
    }

    static boolean puedeConsultarReportes() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(r -> "ROLE_DIRECCION".equals(r) || "ROLE_JEFE_AREA".equals(r));
    }
}
