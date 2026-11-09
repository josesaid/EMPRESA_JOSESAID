package com.mx.josesaid.develop.ui.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.ui.nexus.NexusCliente;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AsistenteNexusRutasTest {

    private final NexusCliente nexus = mock(NexusCliente.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new AsistenteNexusController(nexus)).build();

    @Test
    void postDeLaPreguntaRedirigeALaPantalla() throws Exception {
        when(nexus.consultar(anyString())).thenReturn("ok");

        mvc.perform(post("/asistente").param("pregunta", "hola"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/asistente"));
    }

    @Test
    void getDeLaPantallaResponde() throws Exception {
        mvc.perform(get("/asistente")).andExpect(status().isOk());
    }

    @Test
    void postDeLimpiarRedirigeALaPantalla() throws Exception {
        mvc.perform(post("/asistente/limpiar"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/asistente"));
    }
}
