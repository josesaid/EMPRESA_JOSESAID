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

    private static final Set<String> ROLES_REPORTES = Set.of("ROLE_DIRECCION");
    private static final Set<String> ROLES_CLIENTES = Set.of("ROLE_VENTAS", "ROLE_ADMIN");
    private static final Set<String> ROLES_PEDIDOS = Set.of("ROLE_VENTAS", "ROLE_DIRECCION");
    private static final Set<String> ROLES_USUARIOS = Set.of("ROLE_ADMIN");
    private static final Set<String> ROLES_DOCUMENTOS = Set.of("ROLE_LEGAL", "ROLE_DIRECCION");
    private static final Set<String> ROLES_EXISTENCIAS = Set.of("ROLE_ALMACEN", "ROLE_COMPRAS", "ROLE_VENTAS");
    private static final Set<String> ROLES_TRAZABILIDAD = Set.of("ROLE_OPERACIONES", "ROLE_CALIDAD");
    private static final Set<String> ROLES_CONTABILIDAD = Set.of("ROLE_CONTADOR", "ROLE_DIRECCION");
    private static final Set<String> ROLES_COMPROBANTES = Set.of("ROLE_FINANZAS", "ROLE_VENTAS");
    private static final Set<String> ROLES_PROVEEDORES = Set.of("ROLE_COMPRAS", "ROLE_FINANZAS");
    private static final Set<String> ROLES_ORDENES_COMPRA = Set.of("ROLE_COMPRAS", "ROLE_ALMACEN", "ROLE_DIRECCION");

    private AlcanceHerramientas() {
    }

    static boolean puedeConsultarReportes() {
        return tieneAlgunRol(ROLES_REPORTES);
    }

    static boolean puedeConsultarClientes() {
        return tieneAlgunRol(ROLES_CLIENTES);
    }

    static boolean puedeConsultarPedidos() {
        return tieneAlgunRol(ROLES_PEDIDOS);
    }

    static boolean puedeConsultarUsuarios() {
        return tieneAlgunRol(ROLES_USUARIOS);
    }

    static boolean puedeConsultarDocumentos() {
        return tieneAlgunRol(ROLES_DOCUMENTOS);
    }

    static boolean puedeConsultarInventario() {
        return tieneAlgunRol(ROLES_EXISTENCIAS) || tieneAlgunRol(ROLES_TRAZABILIDAD);
    }

    static boolean puedeConsultarContabilidad() {
        return tieneAlgunRol(ROLES_CONTABILIDAD);
    }

    static boolean puedeConsultarComprobantes() {
        return tieneAlgunRol(ROLES_COMPROBANTES);
    }

    static boolean puedeConsultarProveedores() {
        return tieneAlgunRol(ROLES_PROVEEDORES);
    }

    static boolean puedeConsultarOrdenesCompra() {
        return tieneAlgunRol(ROLES_ORDENES_COMPRA);
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
