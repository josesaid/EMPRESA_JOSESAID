package com.mx.josesaid.develop.nexus.comun.tokens;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;

/**
 * Advisor que mide tokens y costo de cada llamada al modelo al terminar la respuesta.
 * Espera en el contexto las claves "servicio" y "usuario"; si faltan, usa "desconocido".
 */
public final class RegistroTokensAdvisor implements CallAdvisor {

    public static final String NOMBRE = "registro-tokens";
    public static final String CLAVE_SERVICIO = "servicio";
    public static final String CLAVE_USUARIO = "usuario";

    private final RegistroTokens registro;

    public RegistroTokensAdvisor(RegistroTokens registro) {
        this.registro = registro;
    }

    @Override
    public String getName() {
        return NOMBRE;
    }

    @Override
    public int getOrder() {
        // Después del filtro de datos sensibles: mide la respuesta real del modelo.
        return Integer.MIN_VALUE + 200;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientResponse respuesta = chain.nextCall(request);
        ChatResponse chat = respuesta == null ? null : respuesta.chatResponse();
        Usage uso = chat == null || chat.getMetadata() == null ? null : chat.getMetadata().getUsage();
        if (uso != null) {
            registro.registrar(
                    texto(request.context().get(CLAVE_SERVICIO)),
                    texto(request.context().get(CLAVE_USUARIO)),
                    valor(uso.getPromptTokens()),
                    valor(uso.getCompletionTokens()));
        }
        return respuesta;
    }

    private static String texto(Object valor) {
        return valor == null ? null : valor.toString();
    }

    private static long valor(Integer n) {
        return n == null ? 0L : n.longValue();
    }
}
