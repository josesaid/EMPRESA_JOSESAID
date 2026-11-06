package com.mx.josesaid.develop.documentos.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.documentos.application.DocumentosService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DocumentosControlador.class)
@Import(DocumentosControladorValidacionTest.Configuracion.class)
class DocumentosControladorValidacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DocumentosService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void parametroDemasiadoLargoDa400() throws Exception {
        mvc.perform(get("/v1/documentos").param("titulo", "A".repeat(201))
                        .with(jwt().jwt(j -> j.subject(SUJETO))
                                .authorities(new SimpleGrantedAuthority("ROLE_LEGAL"))))
                .andExpect(status().isBadRequest());
    }
}
