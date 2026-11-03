package com.mx.josesaid.develop.nexus.infrastructure;

import static java.util.stream.Collectors.toUnmodifiableSet;

import java.util.Map;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
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
    private static final Set<String> ROLES_PREPARAR_PEDIDO = Set.of("ROLE_VENTAS");
    private static final Set<String> ROLES_ACCIONES_VENTAS = Set.of("ROLE_VENTAS");
    private static final Set<String> ROLES_USUARIOS = Set.of("ROLE_ADMIN");
    private static final Set<String> ROLES_RESUMEN_VENTAS = Set.of("ROLE_DIRECCION", "ROLE_FINANZAS");
    private static final Set<String> ROLES_DOCUMENTOS = Set.of("ROLE_LEGAL", "ROLE_DIRECCION");
    private static final Set<String> ROLES_EXISTENCIAS = Set.of("ROLE_ALMACEN", "ROLE_COMPRAS", "ROLE_VENTAS");
    private static final Set<String> ROLES_TRAZABILIDAD = Set.of("ROLE_OPERACIONES", "ROLE_CALIDAD");
    private static final Set<String> ROLES_CONTABILIDAD = Set.of("ROLE_CONTADOR", "ROLE_DIRECCION");
    private static final Set<String> ROLES_COMPROBANTES = Set.of("ROLE_FINANZAS", "ROLE_VENTAS");
    private static final Set<String> ROLES_PROVEEDORES = Set.of("ROLE_COMPRAS", "ROLE_FINANZAS");
    private static final Set<String> ROLES_ORDENES_COMPRA = Set.of("ROLE_COMPRAS", "ROLE_ALMACEN", "ROLE_DIRECCION");
    // Roles que aparecen en alguna política. Un rol nuevo debe agregarse aquí y en la política correspondiente.
    private static final Set<String> ROLES_POLITICAS = Set.of("ROLE_COMPRAS", "ROLE_DIRECCION");
    // Acciones por MCP: quien las prepara debe tener uno de estos roles y además presentar el código.
    private static final Set<String> ROLES_ACCIONES_MCP = Set.of("ROLE_CONTADOR", "ROLE_VENTAS");

    /** Rol(es) que pueden usar cada herramienta. Lo que no aparece aquí se niega. */
    private static final Map<String, Set<String>> ROLES_POR_HERRAMIENTA = Map.ofEntries(
            Map.entry("buscar_clientes", ROLES_CLIENTES),
            Map.entry("obtener_cliente", ROLES_CLIENTES),
            Map.entry("consultar_indicadores", ROLES_REPORTES),
            Map.entry("consultar_reporte", ROLES_REPORTES),
            Map.entry("consultar_pedidos", ROLES_PEDIDOS),
            Map.entry("resumen_ventas", ROLES_RESUMEN_VENTAS),
            Map.entry("consultar_proveedor", ROLES_PROVEEDORES),
            Map.entry("consultar_orden_compra", ROLES_ORDENES_COMPRA),
            Map.entry("consultar_comprobante", ROLES_COMPROBANTES),
            Map.entry("consultar_poliza", ROLES_CONTABILIDAD),
            Map.entry("consultar_estados_financieros", ROLES_CONTABILIDAD),
            Map.entry("consultar_existencias", ROLES_EXISTENCIAS),
            Map.entry("consultar_trazabilidad_lote", ROLES_TRAZABILIDAD),
            Map.entry("consultar_documento", ROLES_DOCUMENTOS),
            Map.entry("consultar_documentos_por_vencer", ROLES_DOCUMENTOS),
            Map.entry("consultar_usuario", ROLES_USUARIOS),
            Map.entry("buscar_proveedores", ROLES_PROVEEDORES),
            Map.entry("buscar_comprobante_por_folio", ROLES_COMPROBANTES),
            Map.entry("listar_polizas", ROLES_CONTABILIDAD),
            Map.entry("preparar_pedido_borrador", ROLES_PREPARAR_PEDIDO),
            Map.entry("crear_prospecto_borrador", ROLES_ACCIONES_VENTAS),
            Map.entry("confirmar_pedido", ROLES_ACCIONES_VENTAS),
            Map.entry("cancelar_pedido", ROLES_ACCIONES_VENTAS),
            Map.entry("enviar_cotizacion", ROLES_ACCIONES_VENTAS),
            Map.entry("aceptar_cotizacion", ROLES_ACCIONES_VENTAS),
            Map.entry("convertir_cotizacion_en_pedido", ROLES_ACCIONES_VENTAS),
            Map.entry("crear_cotizacion_borrador", ROLES_ACCIONES_VENTAS),
            Map.entry("reemplazar_lineas_pedido", ROLES_ACCIONES_VENTAS),
            Map.entry("registrar_devolucion_pedido", ROLES_ACCIONES_VENTAS),
            Map.entry("cambiar_estatus_prospecto", ROLES_ACCIONES_VENTAS),
            Map.entry("convertir_prospecto_en_cliente", ROLES_ACCIONES_VENTAS),
            Map.entry("consultar_politicas", ROLES_POLITICAS),
            Map.entry("registrar_interaccion_cliente", ROLES_ACCIONES_VENTAS),
            Map.entry("sugerir_poliza", Set.of("ROLE_CONTADOR")),
            Map.entry("confirmar_accion", ROLES_ACCIONES_MCP),
            Map.entry("cancelar_accion", ROLES_ACCIONES_MCP));

    private AlcanceHerramientas() {
    }

    /** Indica si la herramienta existe en el mapa de roles. */
    static boolean conoce(String herramienta) {
        return ROLES_POR_HERRAMIENTA.containsKey(herramienta);
    }

    /** Indica si el usuario actual tiene un rol que permite usar la herramienta. */
    static boolean puedeUsar(String herramienta) {
        Set<String> roles = ROLES_POR_HERRAMIENTA.get(herramienta);
        return roles != null && tieneAlgunRol(roles);
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

    /** Roles del usuario actual sin el prefijo ROLE_, como los guarda la política. */
    static Set<String> rolesDelUsuario() {
        Authentication autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion == null) {
            return Set.of();
        }
        return autenticacion.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(r -> r.startsWith("ROLE_"))
                .map(r -> r.substring("ROLE_".length()))
                .collect(toUnmodifiableSet());
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
