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
    AsistenteIA asistenteIA(ChatModel modelo, RegistroTokens registro, RepositorioPrompts prompts,
                            HerramientasReportes reportes) {
        return new ClaudeAsistenteIA(modelo, registro, prompts, reportes);
    }
}
