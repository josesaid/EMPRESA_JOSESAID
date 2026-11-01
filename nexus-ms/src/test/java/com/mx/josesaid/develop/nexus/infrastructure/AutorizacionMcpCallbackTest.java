package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.mcp.McpToolUtils;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;

class AutorizacionMcpCallbackTest {

    private final AtomicReference<Authentication> vistoPorHerramienta = new AtomicReference<>();

    private ToolCallback herramienta(String nombre) {
        ToolDefinition definicion = mock(ToolDefinition.class);
        when(definicion.name()).thenReturn(nombre);
        ToolCallback callback = mock(ToolCallback.class);
        when(callback.getToolDefinition()).thenReturn(definicion);
        when(callback.call(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> {
                    vistoPorHerramienta.set(SecurityContextHolder.getContext().getAuthentication());
                    return "resultado";
                });
        when(callback.call(org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(inv -> {
                    vistoPorHerramienta.set(SecurityContextHolder.getContext().getAuthentication());
                    return "resultado";
                });
        return callback;
    }

    private static ToolContext contextoCon(Authentication autenticacion) {
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.transportContext()).thenReturn(
                McpTransportContext.create(Map.of(AutorizacionMcpCallback.CLAVE_AUTENTICACION, autenticacion)));
        return new ToolContext(Map.of(McpToolUtils.TOOL_CONTEXT_MCP_EXCHANGE_KEY, exchange));
    }

    private static Authentication con(String... roles) {
        return new UsernamePasswordAuthenticationToken("usuario-1", null,
                java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList());
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinIdentidadLaHerramientaNoSeEjecuta() {
        ToolCallback delegado = herramienta("consultar_politicas");

        assertThatThrownBy(() -> new AutorizacionMcpCallback(delegado).call("{}", new ToolContext(Map.of())))
                .isInstanceOf(AutorizacionMcpCallback.SinPermisoMcpException.class)
                .hasMessage(AutorizacionMcpCallback.DENEGADO);
        assertThat(vistoPorHerramienta.get()).isNull();
    }

    @Test
    void conRolSinPermisoLaHerramientaSeNiegaSinEjecutarse() {
        ToolCallback delegado = herramienta("consultar_politicas");

        assertThatThrownBy(() -> new AutorizacionMcpCallback(delegado).call("{}", contextoCon(con("ROLE_VENTAS"))))
                .isInstanceOf(AutorizacionMcpCallback.SinPermisoMcpException.class);
        assertThat(vistoPorHerramienta.get()).isNull();
    }

    @Test
    void conRolPermitidoSeEjecutaConLaIdentidadDelClienteYRestauraElContexto() {
        ToolCallback delegado = herramienta("consultar_politicas");
        SecurityContextHolder.getContext().setAuthentication(con("ROLE_LEGAL"));
        Authentication cliente = con("ROLE_DIRECCION");

        String respuesta = new AutorizacionMcpCallback(delegado).call("{}", contextoCon(cliente));

        assertThat(respuesta).isEqualTo("resultado");
        assertThat(vistoPorHerramienta.get()).isSameAs(cliente);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(a -> a.getAuthority()).containsExactly("ROLE_LEGAL");
    }

    @Test
    void unaHerramientaDesconocidaSeNiega() {
        ToolCallback delegado = herramienta("herramienta_inventada");

        assertThatThrownBy(() -> new AutorizacionMcpCallback(delegado).call("{}", contextoCon(con("ROLE_DIRECCION"))))
                .isInstanceOf(AutorizacionMcpCallback.SinPermisoMcpException.class);
        assertThat(vistoPorHerramienta.get()).isNull();
    }
}
