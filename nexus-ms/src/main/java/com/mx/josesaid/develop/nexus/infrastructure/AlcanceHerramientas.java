package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Decide qué herramientas se ofrecen al modelo según los roles del usuario que pregunta.
 * Si el usuario no tiene el rol, la herramienta no se ofrece: el modelo no puede pedirla.
 * Cada servicio vuelve a validar el rol con el mismo token.
 */
final class AlcanceHerramientas {

    private static final Set<String> ROLES_REPORTES = Set.of("ROLE_DIRECCION", "ROLE_JEFE_AREA");
    private static final Set<String> ROLES_CLIENTES = Set.of("ROLE_VENTAS", "ROLE_ADMIN");

    private AlcanceHerramientas() {
    }

    static boolean puedeConsultarReportes() {
        return tieneAlgunRol(ROLES_REPORTES);
    }

    static boolean puedeConsultarClientes() {
        return tieneAlgunRol(ROLES_CLIENTES);
    }

    private static boolean tieneAlgunRol(Set<String> roles) {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return false;
        }
        return autenticacion.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .anyMatch(roles::contains);
    }
}
