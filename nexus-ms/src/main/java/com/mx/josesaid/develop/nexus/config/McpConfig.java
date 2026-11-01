package com.mx.josesaid.develop.nexus.config;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.ai.mcp.server.webmvc.transport.WebMvcStreamableServerTransportProvider;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacion;
import com.mx.josesaid.develop.nexus.application.ConfirmacionAcciones;
import com.mx.josesaid.develop.nexus.application.ServicioPolizas;
import com.mx.josesaid.develop.nexus.infrastructure.AutorizacionMcpCallback;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasAccionesMcp;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasContabilidad;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasPoliticas;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasPolizas;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonDefaults;

/**
 * Servidor MCP de Nexus (endpoint /mcp, transporte streamable). Expone lectura y preparación de acciones.
 * Cada llamada viaja con el JWT del cliente: nunca hay sesión anónima ni confirmación sin código.
 */
@Configuration
public class McpConfig {

    private static final Logger log = LoggerFactory.getLogger(McpConfig.class);

    @Bean
    CodigoConfirmacion codigoConfirmacion(@Value("${nexus.mcp.confirmacion-secreto:}") String secreto) {
        if (secreto == null || secreto.isBlank()) {
            // Sin secreto configurado, los códigos solo sirven dentro de esta instancia y tras reiniciar dejan de valer.
            log.warn("NEXUS_CONFIRMACION_SECRETO no está definido; se usa un secreto aleatorio por instancia");
            byte[] aleatorio = new byte[32];
            new SecureRandom().nextBytes(aleatorio);
            return new CodigoConfirmacion(aleatorio);
        }
        return new CodigoConfirmacion(secreto.getBytes(StandardCharsets.UTF_8));
    }

    @Bean
    ConfirmacionAcciones confirmacionAcciones(AccionesPendientes acciones, CodigoConfirmacion codigos) {
        return new ConfirmacionAcciones(acciones, codigos);
    }

    @Bean
    HerramientasPolizas herramientasPolizas(ObjectProvider<ServicioPolizas> servicio, CodigoConfirmacion codigos) {
        return new HerramientasPolizas(servicio::getObject, codigos);
    }

    @Bean
    HerramientasAccionesMcp herramientasAccionesMcp(ConfirmacionAcciones confirmaciones, AccionesPendientes acciones) {
        return new HerramientasAccionesMcp(confirmaciones, acciones);
    }

    /** Herramientas que el servidor MCP publica. Cada una pasa por AutorizacionMcpCallback. */
    @Bean
    List<ToolCallback> herramientasMcp(HerramientasPoliticas politicas, HerramientasContabilidad contabilidad,
                                       HerramientasPolizas polizas, HerramientasAccionesMcp acciones) {
        return Arrays.stream(ToolCallbacks.from(politicas, contabilidad, polizas, acciones))
                .map(AutorizacionMcpCallback::new)
                .map(ToolCallback.class::cast)
                .toList();
    }

    @Bean
    WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            @Value("${spring.ai.mcp.server.streamable-http.mcp-endpoint:/mcp}") String endpoint) {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(McpJsonDefaults.getMapper())
                .mcpEndpoint(endpoint)
                .contextExtractor(McpConfig::extraerContexto)
                .build();
    }

    /** Toma la identidad ya autenticada por Spring Security y la pasa a cada llamada de herramienta. */
    static McpTransportContext extraerContexto(ServerRequest solicitud) {
        return solicitud.principal()
                .filter(Authentication.class::isInstance)
                .map(p -> McpTransportContext.create(
                        Map.<String, Object>of(AutorizacionMcpCallback.CLAVE_AUTENTICACION, p)))
                .orElse(McpTransportContext.EMPTY);
    }
}
