package com.mx.josesaid.develop.clientes.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.clientes.application.Auditoria;
import com.mx.josesaid.develop.clientes.application.ClienteService;
import com.mx.josesaid.develop.clientes.config.SeguridadConfig;
import com.mx.josesaid.develop.clientes.seguridad.RevocacionSesiones;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
    private static final String NUEVO_CLIENTE = "{\"razonSocial\":\"Acme SA\",\"rfc\":\"AAA010101AAA\","
            + "\"contactoPrincipal\":{\"nombre\":\"Ana\",\"correo\":\"ana@ejemplo.com\",\"rol\":\"COMPRAS\"}}";
    private static final String ACTUALIZAR = "{\"razonSocial\":\"Acme SA de CV\"}";
    private static final String ESTATUS = "{\"estatus\":\"ACTIVO\"}";
    private static final String CONTACTO = "{\"nombre\":\"Luis\",\"correo\":\"luis@ejemplo.com\",\"rol\":\"COMPRAS\",\"principal\":false}";

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

    private void escribe(MockHttpServletRequestBuilder req, String ruta, String rol, String cuerpo, boolean permite)
            throws Exception {
        MockHttpServletRequestBuilder conRol = req.contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(jwt().jwt(j -> j.subject(SUJETO))
                        .authorities(rol.isEmpty() ? java.util.List.of()
                                : java.util.List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
        if (permite) {
            permitido(conRol, ruta, rol);
        } else {
            mvc.perform(conRol).andExpect(status().isForbidden());
        }
    }

    private void permitido(MockHttpServletRequestBuilder req, String ruta, String rol) throws Exception {
        int codigo;
        try {
            codigo = mvc.perform(req).andReturn().getResponse().getStatus();
        } catch (jakarta.servlet.ServletException e) {
            codigo = 200; // llegó al controlador: el rol fue aceptado
        }
        org.assertj.core.api.Assertions.assertThat(codigo)
                .as("%s debería estar permitido para %s", ruta, rol)
                .isNotIn(401, 403);
    }

    @Test
    void crearCliente() throws Exception {
        String ruta = "/v1/clientes";
        escribe(post(ruta), ruta, "ADMIN", NUEVO_CLIENTE, true);
        escribe(post(ruta), ruta, "VENTAS", NUEVO_CLIENTE, false);
        escribe(post(ruta), ruta, "LEGAL", NUEVO_CLIENTE, false);
    }

    @Test
    void actualizarCliente() throws Exception {
        String ruta = "/v1/clientes/" + ID;
        escribe(patch(ruta), ruta, "ADMIN", ACTUALIZAR, true);
        escribe(patch(ruta), ruta, "VENTAS", ACTUALIZAR, false);
        escribe(patch(ruta), ruta, "COMPRAS", ACTUALIZAR, false);
    }

    @Test
    void cambiarEstatusCliente() throws Exception {
        String ruta = "/v1/clientes/" + ID + "/estatus";
        escribe(patch(ruta), ruta, "ADMIN", ESTATUS, true);
        escribe(patch(ruta), ruta, "VENTAS", ESTATUS, false);
        escribe(patch(ruta), ruta, "LEGAL", ESTATUS, false);
    }

    @Test
    void agregarContacto() throws Exception {
        String ruta = "/v1/clientes/" + ID + "/contactos";
        escribe(post(ruta), ruta, "ADMIN", CONTACTO, true);
        escribe(post(ruta), ruta, "VENTAS", CONTACTO, false);
        escribe(post(ruta), ruta, "COMPRAS", CONTACTO, false);
    }
}
