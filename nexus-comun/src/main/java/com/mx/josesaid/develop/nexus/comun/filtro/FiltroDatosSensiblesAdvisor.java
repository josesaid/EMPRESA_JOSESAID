package com.mx.josesaid.develop.nexus.comun.filtro;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

/**
 * Advisor que redacta los mensajes de usuario antes de llamar al modelo.
 * Los mensajes de sistema y del asistente no se modifican.
 */
public final class FiltroDatosSensiblesAdvisor implements CallAdvisor {

    public static final String NOMBRE = "filtro-datos-sensibles";

    /**
     * Clave del contexto de la petición. Si vale {@code true}, los correos del mensaje del usuario no se redactan.
     * Solo lo activa el asistente para un administrador que pide un alta: el correo es el dato que la herramienta necesita.
     */
    public static final String CLAVE_CORREO_PERMITIDO = "correoPermitido";

    /** Se ejecuta primero, antes de cualquier otro advisor que pueda leer el prompt. */
    private static final int ORDEN = Integer.MIN_VALUE + 100;

    @Override
    public String getName() {
        return NOMBRE;
    }

    @Override
    public int getOrder() {
        return ORDEN;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        Prompt original = request.prompt();
        boolean correoPermitido = Boolean.TRUE.equals(request.context().get(CLAVE_CORREO_PERMITIDO));
        EnumSet<FiltroDatosSensibles.Tipo> omitir = EnumSet.of(FiltroDatosSensibles.Tipo.RFC);
        if (correoPermitido) {
            omitir.add(FiltroDatosSensibles.Tipo.CORREO);
        }
        List<Message> nuevos = new ArrayList<>();
        for (Message mensaje : original.getInstructions()) {
            if (mensaje instanceof UserMessage usuario) {
                String redactado = FiltroDatosSensibles.redactar(usuario.getText(), omitir).texto();
                nuevos.add(new UserMessage(redactado));
            } else {
                nuevos.add(mensaje);
            }
        }
        Prompt filtrado = new Prompt(nuevos, original.getOptions());
        return chain.nextCall(new ChatClientRequest(filtrado, request.context()));
    }
}
