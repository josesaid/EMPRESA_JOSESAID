package com.mx.josesaid.develop.nexus.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.nexus.application.AccionNoPendienteException;
import com.mx.josesaid.develop.nexus.application.AccionPendiente;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.PresupuestoAgotadoException;
import com.mx.josesaid.develop.nexus.application.PresupuestoTokens;
import java.time.Instant;
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
import org.testcontainers.utility.DockerImageName;
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
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(DockerImageName.parse("pgvector/pgvector:pg17").asCompatibleSubstituteFor("postgres"));

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ChatModel modelo;

    @MockitoBean
    private AccionesPendientes acciones;

    @MockitoBean
    private PresupuestoTokens presupuesto;

    private static final String ACCION_ID = "7d3c1f2e-0000-4000-8000-000000000001";

    private static org.springframework.security.core.authority.SimpleGrantedAuthority rol(String nombre) {
        return new org.springframework.security.core.authority.SimpleGrantedAuthority(nombre);
    }

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
    void usuarioAutenticadoSinRolDeAreaResponde200() throws Exception {
        when(modelo.getOptions()).thenReturn(AnthropicChatOptions.builder().build());
        when(modelo.call(any(Prompt.class))).thenReturn(respuesta("No tengo herramientas para tu rol."));
        mvc.perform(post("/nexus/consultas").contentType("application/json")
                        .content("{\"pregunta\":\"Hola\"}")
                        .with(jwt().jwt(j -> j.subject("u9")).authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_OTRO"))))
                .andExpect(status().isOk());
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
    void confirmarSinTokenResponde401() throws Exception {
        mvc.perform(post("/nexus/acciones/" + ACCION_ID + "/confirmacion"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmarSinRolVentasResponde403() throws Exception {
        mvc.perform(post("/nexus/acciones/" + ACCION_ID + "/confirmacion")
                        .with(jwt().jwt(j -> j.subject("u-compras")).authorities(rol("ROLE_COMPRAS"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmarPropiaAccionResponde201() throws Exception {
        when(acciones.confirmar(java.util.UUID.fromString(ACCION_ID), "u-ventas"))
                .thenReturn("{\"id\":\"ped-1\"}");
        mvc.perform(post("/nexus/acciones/" + ACCION_ID + "/confirmacion")
                        .with(jwt().jwt(j -> j.subject("u-ventas")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("ped-1"));
    }

    @Test
    void confirmarAccionAjenaResponde404() throws Exception {
        when(acciones.confirmar(java.util.UUID.fromString(ACCION_ID), "u-otro"))
                .thenThrow(new com.mx.josesaid.develop.nexus.application.AccionNoEncontradaException("Acción no encontrada"));
        mvc.perform(post("/nexus/acciones/" + ACCION_ID + "/confirmacion")
                        .with(jwt().jwt(j -> j.subject("u-otro")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmarAccionYaConfirmadaResponde409() throws Exception {
        when(acciones.confirmar(java.util.UUID.fromString(ACCION_ID), "u-ventas"))
                .thenThrow(new com.mx.josesaid.develop.nexus.application.AccionNoPendienteException("La acción ya está confirmada"));
        mvc.perform(post("/nexus/acciones/" + ACCION_ID + "/confirmacion")
                        .with(jwt().jwt(j -> j.subject("u-ventas")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isConflict());
    }

    @Test
    void consultarAccionPropiaMuestraResumenSinPayload() throws Exception {
        java.util.UUID id = java.util.UUID.fromString(ACCION_ID);
        when(acciones.consultar(id, "u-ventas")).thenReturn(new AccionPendiente(id, "u-ventas",
                "PEDIDO_BORRADOR", "{\"clienteId\":\"c1\"}", "Pedido para c1", "PENDIENTE",
                Instant.now(), Instant.now().plusSeconds(600)));
        mvc.perform(get("/nexus/acciones/" + ACCION_ID)
                        .with(jwt().jwt(j -> j.subject("u-ventas")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resumen").value("Pedido para c1"))
                .andExpect(jsonPath("$.estatus").value("PENDIENTE"))
                .andExpect(jsonPath("$.payload").doesNotExist());
    }

    @Test
    void consultarAccionAjenaResponde404() throws Exception {
        when(acciones.consultar(java.util.UUID.fromString(ACCION_ID), "u-otro"))
                .thenThrow(new com.mx.josesaid.develop.nexus.application.AccionNoEncontradaException("Acción no encontrada"));
        mvc.perform(get("/nexus/acciones/" + ACCION_ID)
                        .with(jwt().jwt(j -> j.subject("u-otro")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelarPropiaAccionResponde204() throws Exception {
        mvc.perform(post("/nexus/acciones/" + ACCION_ID + "/cancelacion")
                        .with(jwt().jwt(j -> j.subject("u-ventas")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void cancelarAccionYaConfirmadaResponde409() throws Exception {
        doThrow(new AccionNoPendienteException("La acción ya está confirmada"))
                .when(acciones).cancelar(java.util.UUID.fromString(ACCION_ID), "u-ventas");
        mvc.perform(post("/nexus/acciones/" + ACCION_ID + "/cancelacion")
                        .with(jwt().jwt(j -> j.subject("u-ventas")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isConflict());
    }

    @Test
    void preguntaVaciaResponde400() throws Exception {
        mvc.perform(post("/nexus/consultas").contentType("application/json")
                        .content("{\"pregunta\":\"\"}")
                        .with(jwt().jwt(j -> j.subject("u1")).authorities(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_VENTAS"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void presupuestoAgotadoResponde429SinLlamarAlModelo() throws Exception {
        doThrow(new PresupuestoAgotadoException("Alcanzaste tu límite diario de tokens."))
                .when(presupuesto).exigirDisponible("u-ventas");
        mvc.perform(post("/nexus/consultas").contentType("application/json")
                        .content("{\"pregunta\":\"Hola\"}")
                        .with(jwt().jwt(j -> j.subject("u-ventas")).authorities(rol("ROLE_VENTAS"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.detail").value("Alcanzaste tu límite diario de tokens."));
        verify(modelo, never()).call(any(Prompt.class));
    }
}
