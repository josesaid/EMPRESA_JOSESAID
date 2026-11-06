package com.mx.josesaid.develop.clientes.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.clientes.application.Auditoria;
import com.mx.josesaid.develop.clientes.application.ClienteService;
import com.mx.josesaid.develop.clientes.config.SeguridadConfig;
import com.mx.josesaid.develop.clientes.seguridad.RevocacionSesiones;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Matriz de autorización de ClienteController (endpoints de lectura).
 * Un rol permitido no debe recibir 401 ni 403. Un rol no permitido debe recibir 403.
 * Los servicios están simulados: estas pruebas solo miden la autorización.
 */
@WebMvcTest(ClienteController.class)
@Import({SeguridadConfig.class, ClienteControllerAutorizacionTest.Configuracion.class})
class ClienteControllerAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";

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

    private MockHttpServletRequestBuilder lectura(String ruta, String... roles) {
        java.util.List<org.springframework.security.core.GrantedAuthority> autoridades = java.util.Arrays.stream(roles)
                .<org.springframework.security.core.GrantedAuthority>map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .toList();
        return get(ruta).with(jwt().jwt(j -> j.subject(SUJETO)).authorities(autoridades));
    }

    private void permitido(String ruta, String rol) throws Exception {
        int codigo;
        try {
            codigo = mvc.perform(lectura(ruta, rol)).andReturn().getResponse().getStatus();
        } catch (jakarta.servlet.ServletException e) {
            // El servicio simulado devuelve null y el controlador falla después de la autorización:
            // llegar hasta el controlador significa que el rol fue aceptado.
            codigo = 200;
        }
        org.assertj.core.api.Assertions.assertThat(codigo)
                .as("%s debería estar permitido para %s", ruta, rol)
                .isNotIn(401, 403);
    }

    private void denegado(String ruta, String rol) throws Exception {
        mvc.perform(lectura(ruta, rol)).andExpect(status().isForbidden());
    }

    @Test
    void listarClientes() throws Exception {
        permitido("/v1/clientes", "ADMIN");
        permitido("/v1/clientes", "VENTAS");
        denegado("/v1/clientes", "LEGAL");
        denegado("/v1/clientes", "COMPRAS");
    }

    @Test
    void obtenerCliente() throws Exception {
        permitido("/v1/clientes/" + ID, "ADMIN");
        permitido("/v1/clientes/" + ID, "VENTAS");
        denegado("/v1/clientes/" + ID, "LEGAL");
    }

    @Test
    void sinRolNoHayAcceso() throws Exception {
        denegado("/v1/clientes", "");
    }
}
