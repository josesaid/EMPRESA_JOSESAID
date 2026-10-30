package com.mx.josesaid.develop.nexus.infrastructure;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.PreguntaAsistente;
import com.mx.josesaid.develop.nexus.application.RespuestaAsistente;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensiblesAdvisor;
import com.mx.josesaid.develop.nexus.comun.prompts.PromptVersionado;
import com.mx.josesaid.develop.nexus.comun.prompts.RepositorioPrompts;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokens;
import com.mx.josesaid.develop.nexus.comun.tokens.RegistroTokensAdvisor;

/**
 * Adaptador del puerto AsistenteIA sobre Claude, con ChatClient de Spring AI.
 * Los datos sensibles se redactan antes de llamar al modelo y el consumo se mide por llamada.
 */
public final class ClaudeAsistenteIA implements AsistenteIA {

    static final String SERVICIO = "nexus-ms";
    static final String PROMPT = "asistente-base";

    private final ChatClient cliente;

    public ClaudeAsistenteIA(ChatModel modelo, RegistroTokens registro, RepositorioPrompts prompts) {
        PromptVersionado prompt = prompts.obtener(PROMPT);
        this.cliente = ChatClient.builder(modelo)
                .defaultSystem(prompt.texto())
                .defaultAdvisors(
                        new FiltroDatosSensiblesAdvisor(),
                        new RegistroTokensAdvisor(registro))
                .build();
    }

    @Override
    public RespuestaAsistente consultar(PreguntaAsistente pregunta) {
        String texto = cliente.prompt()
                .user(pregunta.texto())
                .advisors(a -> a
                        .param(RegistroTokensAdvisor.CLAVE_SERVICIO, SERVICIO)
                        .param(RegistroTokensAdvisor.CLAVE_USUARIO, pregunta.usuario()))
                .call()
                .content();
        return new RespuestaAsistente(texto);
    }
}
