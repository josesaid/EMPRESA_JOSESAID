package com.mx.josesaid.develop.clientes.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.clientes.application.Auditoria;
import com.mx.josesaid.develop.clientes.application.ClienteService;
import com.mx.josesaid.develop.clientes.config.SeguridadConfig;
import com.mx.josesaid.develop.clientes.seguridad.RevocacionSesiones;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ClienteController.class)
@Import({SeguridadConfig.class, ClienteControllerValidacionTest.Configuracion.class})
class ClienteControllerValidacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService servicio;

    @MockitoBean
    private Auditoria auditoria;

    @MockitoBean
    private Presentacion presentacion;

    @MockitoBean
    private RevocacionSesiones revocaciones;

    @Test
    void rfcDeMasDe13CaracteresDa400() throws Exception {
        mvc.perform(get("/v1/clientes").param("rfc", "X".repeat(14))
                        .with(jwt().jwt(j -> j.subject(SUJETO)).authorities(new SimpleGrantedAuthority("ROLE_VENTAS"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nombreDeMasDe200CaracteresDa400() throws Exception {
        mvc.perform(get("/v1/clientes").param("nombre", "A".repeat(201))
                        .with(jwt().jwt(j -> j.subject(SUJETO)).authorities(new SimpleGrantedAuthority("ROLE_VENTAS"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nombreDe200CaracteresEsValido() throws Exception {
        when(servicio.buscar(any(), any(), any())).thenReturn(new PageImpl<>(List.of()));
        mvc.perform(get("/v1/clientes").param("nombre", "A".repeat(200))
                        .with(jwt().jwt(j -> j.subject(SUJETO)).authorities(new SimpleGrantedAuthority("ROLE_VENTAS"))))
                .andExpect(status().isOk());
    }
}
