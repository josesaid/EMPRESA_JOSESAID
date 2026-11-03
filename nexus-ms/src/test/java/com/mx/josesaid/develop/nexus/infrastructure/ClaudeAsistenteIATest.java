package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.client.RestClient;

import com.mx.josesaid.develop.nexus.application.AlmacenFragmentos;
import com.mx.josesaid.develop.nexus.application.FragmentoIndexado;
import com.mx.josesaid.develop.nexus.application.PreguntaAsistente;
import com.mx.josesaid.develop.nexus.application.RecuperadorPoliticas;
import com.mx.josesaid.develop.nexus.application.ResultadoPolitica;
import com.mx.josesaid.develop.nexus.comun.prompts.RepositorioPrompts;
import com.mx.josesaid.develop.nexus.comun.tokens.PreciosTokens;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokens;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

class ClaudeAsistenteIATest {

    private final List<Prompt> enviados = new ArrayList<>();
    private String respuestaSimulada = "Vencen 3 contratos.";
    private ClaudeAsistenteIA asistente;

    @BeforeEach
    void preparar() {
        ChatModel modelo = new ChatModel() {
            @Override
            public ChatResponse call(Prompt prompt) {
                enviados.add(prompt);
                return new ChatResponse(List.of(new Generation(new AssistantMessage(respuestaSimulada))));
            }
        };
        RestClient sinRed = RestClient.create("http://no-usado");
        var registro = new RegistroTokens(new SimpleMeterRegistry(), PreciosTokens.SIN_PRECIO);
        asistente = new ClaudeAsistenteIA(modelo, registro, new RepositorioPrompts(),
                new HerramientasReportes(sinRed), new HerramientasClientes(sinRed),
                new HerramientasVentas(sinRed), new HerramientasCompras(sinRed),
                new HerramientasFinanzas(sinRed), new HerramientasContabilidad(sinRed),
                new HerramientasInventario(sinRed), new HerramientasDocumentos(sinRed),
                new HerramientasUsuarios(sinRed), new HerramientasPoliticas(recuperadorVacio()));
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    /** Recuperador sin fragmentos: la herramienta de políticas se registra pero no se usa en estas pruebas. */
    private static RecuperadorPoliticas recuperadorVacio() {
        AlmacenFragmentos almacen = new AlmacenFragmentos() {
            @Override
            public void reemplazar(String politicaId, List<FragmentoIndexado> fragmentos) {
            }

            @Override
            public List<ResultadoPolitica> buscar(Set<String> roles, float[] consulta, LocalDate hoy, int limite) {
                return List.of();
            }
        };
        return new RecuperadorPoliticas((textos, tipo) -> List.of(), almacen, Clock.systemUTC());
    }

    @Test
    void devuelveLaRespuestaDelModelo() {
        var respuesta = asistente.consultar(new PreguntaAsistente("u1", "¿Qué contratos vencen?"));

        assertThat(respuesta.texto()).isEqualTo("Vencen 3 contratos.");
    }

    @Test
    void dejaElRfcAlModeloYRedactaElCorreo() {
        asistente.consultar(new PreguntaAsistente("u1", "Revisa el RFC XAXX010101000 y escribe a a@b.com"));

        String enviado = enviados.get(0).getUserMessage().getText();
        assertThat(enviado).contains("XAXX010101000").doesNotContain("a@b.com");
        assertThat(enviado).contains("[CORREO]");
    }

    @Test
    void incluyeElPromptVersionadoComoInstruccionDeSistema() {
        asistente.consultar(new PreguntaAsistente("u1", "Hola"));

        String sistema = enviados.get(0).getInstructions().stream()
                .filter(m -> m.getMessageType().name().equals("SYSTEM"))
                .map(m -> m.getText())
                .findFirst()
                .orElse("");
        assertThat(sistema).contains("Eres Nexus", "Responde siempre en español");
    }

    @Test
    void sinAutenticacionNoSeOfreceNingunaHerramienta() {
        assertThat(AlcanceHerramientas.puedeConsultarReportes()).isFalse();
        assertThat(AlcanceHerramientas.puedeConsultarClientes()).isFalse();
    }

    @Test
    void direccionPuedeConsultarReportesPeroNoClientes() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("u1", null, "ROLE_DIRECCION"));

        assertThat(AlcanceHerramientas.puedeConsultarReportes()).isTrue();
        assertThat(AlcanceHerramientas.puedeConsultarClientes()).isFalse();
    }

    @Test
    void ventasPuedeConsultarClientesPeroNoReportes() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("u2", null, "ROLE_VENTAS"));

        assertThat(AlcanceHerramientas.puedeConsultarClientes()).isTrue();
        assertThat(AlcanceHerramientas.puedeConsultarReportes()).isFalse();
    }

    @Test
    void jefeDeAreaNoPuedeConsultarReportesNiClientesDesdeNexus() {
        SecurityContextHolder.getContext().setAuthentication(
                new TestingAuthenticationToken("u3", null, "ROLE_JEFE_AREA"));

        assertThat(AlcanceHerramientas.puedeConsultarReportes()).isFalse();
        assertThat(AlcanceHerramientas.puedeConsultarClientes()).isFalse();
    }
}
