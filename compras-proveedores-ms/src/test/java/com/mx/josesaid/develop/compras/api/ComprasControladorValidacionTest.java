package com.mx.josesaid.develop.compras.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.compras.application.ComprasService;
import com.mx.josesaid.develop.compras.infrastructure.idempotencia.Idempotencia;
import java.util.List;
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

@WebMvcTest(ComprasControlador.class)
@Import(ComprasControladorValidacionTest.Configuracion.class)
class ComprasControladorValidacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ComprasService servicio;

    @MockitoBean
    private Idempotencia idempotencia;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void razonSocialDeMasDe250CaracteresDa400() throws Exception {
        mvc.perform(get("/v1/proveedores").param("razonSocial", "A".repeat(251))
                        .with(jwt().jwt(j -> j.subject(SUJETO))
                                .authorities(new SimpleGrantedAuthority("ROLE_COMPRAS"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void razonSocialDe250CaracteresEsValida() throws Exception {
        when(servicio.buscarProveedores(any())).thenReturn(List.of());
        mvc.perform(get("/v1/proveedores").param("razonSocial", "A".repeat(250))
                        .with(jwt().jwt(j -> j.subject(SUJETO))
                                .authorities(new SimpleGrantedAuthority("ROLE_COMPRAS"))))
                .andExpect(status().isOk());
    }
}
