package com.mx.josesaid.develop.usuarios_accesos.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.usuarios_accesos.application.UsuarioService;
import com.mx.josesaid.develop.usuarios_accesos.config.PropiedadesJwt;
import com.mx.josesaid.develop.usuarios_accesos.config.SeguridadConfig;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Matriz de autorización de UsuarioController: todo /v1/usuarios exige ROLE_ADMIN.
 */
@WebMvcTest(UsuarioController.class)
@Import({SeguridadConfig.class, UsuarioControllerAutorizacionTest.Configuracion.class})
class UsuarioControllerAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private UsuarioService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    // Beans de claves: se simulan para no cargar material criptográfico en esta prueba de autorización.
    @MockitoBean
    private PropiedadesJwt propiedadesJwt;

    @MockitoBean
    private RSAKey claveRsa;

    @MockitoBean
    private JWKSet clavesPublicas;

    @MockitoBean
    private JwtEncoder jwtEncoder;

    private MockHttpServletRequestBuilder peticion(MockHttpServletRequestBuilder base, String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return base.with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
                .jwt().jwt(j -> j.subject(SUJETO)).authorities(autoridades));
    }

    private void permitido(MockHttpServletRequestBuilder req, String descripcion, String rol) throws Exception {
        int codigo;
        try {
            codigo = mvc.perform(req).andReturn().getResponse().getStatus();
        } catch (jakarta.servlet.ServletException e) {
            codigo = 200; // llegó al controlador: el rol fue aceptado
        }
        Assertions.assertThat(codigo)
                .as("%s debería estar permitido para %s", descripcion, rol)
                .isNotIn(401, 403);
    }

    @Test
    void usuariosSoloParaAdmin() throws Exception {
        String ruta = "/v1/usuarios";
        permitido(peticion(get(ruta), "ADMIN"), "GET usuarios", "ADMIN");
        mvc.perform(peticion(get(ruta), "VENTAS")).andExpect(status().isForbidden());
        mvc.perform(peticion(get(ruta), "CONTADOR")).andExpect(status().isForbidden());
        mvc.perform(peticion(get(ruta), "")).andExpect(status().isForbidden());
    }
}
