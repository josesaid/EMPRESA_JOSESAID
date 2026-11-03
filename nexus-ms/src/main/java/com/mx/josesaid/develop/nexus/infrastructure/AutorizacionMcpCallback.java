package com.mx.josesaid.develop.nexus.infrastructure;

import java.util.Map;
import java.util.Optional;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Envuelve una herramienta expuesta por MCP. Restaura en el hilo de la llamada la identidad que
 * llegó con el token del cliente, aplica el mapa de roles y, al terminar, devuelve el contexto anterior.
 * Sin identidad o sin rol, la herramienta no se ejecuta.
 */
public final class AutorizacionMcpCallback implements ToolCallback {

    public static final String CLAVE_AUTENTICACION = "nexus.autenticacion";
    static final String DENEGADO = "Sin permiso para usar esta herramienta con tu rol o sesión actual.";

    private final ToolCallback delegado;

    public AutorizacionMcpCallback(ToolCallback delegado) {
        this.delegado = delegado;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegado.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegado.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, ToolContext toolContext) {
        String nombre = delegado.getToolDefinition().name();
        Optional<Authentication> autenticacion = identidad(toolContext);
        if (autenticacion.isEmpty()) {
            throw new SinPermisoMcpException(DENEGADO);
        }
        SecurityContext anterior = SecurityContextHolder.getContext();
        SecurityContext durante = SecurityContextHolder.createEmptyContext();
        durante.setAuthentication(autenticacion.get());
        SecurityContextHolder.setContext(durante);
        try {
            if (!AlcanceHerramientas.puedeUsar(nombre)) {
                throw new SinPermisoMcpException(DENEGADO);
            }
            return toolContext == null ? delegado.call(toolInput) : delegado.call(toolInput, toolContext);
        } finally {
            SecurityContextHolder.setContext(anterior);
        }
    }

    /** Lanzarla hace que la respuesta MCP llegue con isError=true, no como texto de éxito. */
    public static final class SinPermisoMcpException extends RuntimeException {
        SinPermisoMcpException(String mensaje) {
            super(mensaje);
        }
    }

    private static Optional<Authentication> identidad(ToolContext toolContext) {
        if (toolContext == null) {
            return Optional.empty();
        }
        return McpToolUtils.getMcpExchange(toolContext)
                .map(ex -> ex.transportContext().get(CLAVE_AUTENTICACION))
                .filter(Authentication.class::isInstance)
                .map(Authentication.class::cast);
    }
}
