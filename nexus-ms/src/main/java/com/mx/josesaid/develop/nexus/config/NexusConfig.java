package com.mx.josesaid.develop.nexus.config;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.comun.prompts.RepositorioPrompts;
import com.mx.josesaid.develop.nexus.comun.tokens.PreciosTokens;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokens;
import com.mx.josesaid.develop.nexus.infrastructure.ClaudeAsistenteIA;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasClientes;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasCompras;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasFinanzas;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasContabilidad;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasVentas;
import com.mx.josesaid.develop.nexus.infrastructure.HerramientasReportes;

import io.micrometer.core.instrument.MeterRegistry;

@Configuration
public class NexusConfig {

    @Bean
    RepositorioPrompts repositorioPrompts() {
        return new RepositorioPrompts();
    }

    @Bean
    RegistroTokens registroTokens(MeterRegistry registro) {
        // Precios por millón de tokens: se configuran por entorno en un paso posterior.
        return new RegistroTokens(registro, PreciosTokens.SIN_PRECIO);
    }

    @Bean
    HerramientasReportes herramientasReportes(RestClient gatewayClient) {
        return new HerramientasReportes(gatewayClient);
    }

    @Bean
    HerramientasClientes herramientasClientes(RestClient gatewayClient) {
        return new HerramientasClientes(gatewayClient);
    }

    @Bean
    HerramientasVentas herramientasVentas(RestClient gatewayClient) {
        return new HerramientasVentas(gatewayClient);
    }

    @Bean
    HerramientasCompras herramientasCompras(RestClient gatewayClient) {
        return new HerramientasCompras(gatewayClient);
    }

    @Bean
    HerramientasFinanzas herramientasFinanzas(RestClient gatewayClient) {
        return new HerramientasFinanzas(gatewayClient);
    }

    @Bean
    HerramientasContabilidad herramientasContabilidad(RestClient gatewayClient) {
        return new HerramientasContabilidad(gatewayClient);
    }

    @Bean
    AsistenteIA asistenteIA(ChatModel modelo, RegistroTokens registro, RepositorioPrompts prompts,
                            HerramientasReportes reportes, HerramientasClientes clientes,
                            HerramientasVentas ventas, HerramientasCompras compras,
                            HerramientasFinanzas finanzas, HerramientasContabilidad contabilidad) {
        return new ClaudeAsistenteIA(modelo, registro, prompts, reportes, clientes, ventas, compras, finanzas,
                contabilidad);
    }
}
