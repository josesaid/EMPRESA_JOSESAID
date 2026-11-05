package com.mx.josesaid.develop.nexus.comun.filtro;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;

class FiltroDatosSensiblesAdvisorTest {

    private static final String TEXTO = "Prepara el alta de ana@empresa.com";

    @Test
    void redactaCorreoPorDefecto() {
        assertThat(textoQueLlegaAlModelo(Map.of())).isEqualTo("Prepara el alta de [CORREO]");
    }

    @Test
    void dejaPasarCorreoCuandoElContextoLoPermite() {
        assertThat(textoQueLlegaAlModelo(Map.of(FiltroDatosSensiblesAdvisor.CLAVE_CORREO_PERMITIDO, true)))
                .isEqualTo(TEXTO);
    }

    private static String textoQueLlegaAlModelo(Map<String, Object> contexto) {
        ChatClientRequest solicitud = new ChatClientRequest(new Prompt(new UserMessage(TEXTO)), contexto);
        CallAdvisorChain cadena = mock(CallAdvisorChain.class);
        when(cadena.nextCall(any())).thenReturn(mock(ChatClientResponse.class));

        new FiltroDatosSensiblesAdvisor().adviseCall(solicitud, cadena);

        ArgumentCaptor<ChatClientRequest> enviada = ArgumentCaptor.forClass(ChatClientRequest.class);
        verify(cadena).nextCall(enviada.capture());
        return enviada.getValue().prompt().getInstructions().get(0).getText();
    }
}
