package com.mx.josesaid.develop.nexus.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integración con PostgreSQL real (Testcontainers): permisos por rol, auditoría y migraciones.
 * El modelo se simula: esta prueba no hace llamadas a Claude.
 * Requiere Docker. Se ejecuta con: mvn verify (failsafe).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class NexusIT {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ChatModel modelo;

    private static ChatResponse respuesta(String texto) {
        return new ChatResponse(java.util.List.of(new Generation(new AssistantMessage(texto))));
    }

    @Test
    void sinTokenResponde401() throws Exception {
        mvc.perform(post("/nexus/consultas").contentType("application/json")
                        .content("{\"pregunta\":\"Hola\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rolSinAccesoResponde403() throws Exception {
        mvc.perform(post("/nexus/consultas").contentType("application/json")
                        .content("{\"pregunta\":\"Hola\"}")
                        .with(jwt().jwt(j -> j.subject("u9")).authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OTRO"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void consultaValidaRespondeYQuedaAuditada() throws Exception {
        when(modelo.getOptions()).thenReturn(AnthropicChatOptions.builder().build());
        when(modelo.call(any(Prompt.class))).thenReturn(respuesta("Hay 2 contratos por vencer."));
        Integer antes = jdbc.queryForObject("select count(*) from nexus_consultas", Integer.class);

        mvc.perform(post("/nexus/consultas").contentType("application/json")
                        .content("{\"pregunta\":\"¿Qué contratos vencen? Escribe a a@b.com\"}")
                        .with(jwt().jwt(j -> j.subject("u-ventas")).authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VENTAS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.respuesta").value("Hay 2 contratos por vencer."));

        Integer despues = jdbc.queryForObject("select count(*) from nexus_consultas", Integer.class);
        assertThat(despues).isEqualTo(antes + 1);
        String preguntaAuditada = jdbc.queryForObject(
                "select pregunta_redactada from nexus_consultas where usuario = 'u-ventas' order by creado_utc desc limit 1",
                String.class);
        assertThat(preguntaAuditada).doesNotContain("a@b.com").contains("[CORREO]");
    }

    @Test
    void preguntaVaciaResponde400() throws Exception {
        mvc.perform(post("/nexus/consultas").contentType("application/json")
                        .content("{\"pregunta\":\"\"}")
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VENTAS"))))
                .andExpect(status().isBadRequest());
    }
}
