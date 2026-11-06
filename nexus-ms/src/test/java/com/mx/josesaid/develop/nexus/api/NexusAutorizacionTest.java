package com.mx.josesaid.develop.nexus.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.AuditoriaConsultas;
import com.mx.josesaid.develop.nexus.application.CodigoConfirmacion;
import com.mx.josesaid.develop.nexus.application.ConfirmacionAcciones;
import com.mx.josesaid.develop.nexus.application.PresupuestoTokens;
import com.mx.josesaid.develop.nexus.application.ServicioPolizas;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Matriz de autorización de NexusControlador y PolizasControlador.
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 */
@WebMvcTest({NexusControlador.class, PolizasControlador.class})
@Import({com.mx.josesaid.develop.nexus.config.SeguridadConfig.class, NexusAutorizacionTest.Configuracion.class})
class NexusAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String CODIGO = "{\"codigo\":\"12345678\"}";
    private static final String CONSULTA = "{\"pregunta\":\"¿Cuál es el saldo de la cuenta 1000?\"}";
    private static final String SUGERENCIA = "{\"descripcion\":\"Pago de renta de octubre\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private AsistenteIA asistente;

    @MockitoBean
    private AuditoriaConsultas auditoria;

    @MockitoBean
    private AccionesPendientes acciones;

    @MockitoBean
    private PresupuestoTokens presupuesto;

    @MockitoBean
    private ConfirmacionAcciones confirmacion;

    @MockitoBean
    private ServicioPolizas servicioPolizas;

    @MockitoBean
    private CodigoConfirmacion codigos;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockHttpServletRequestBuilder peticion(MockHttpServletRequestBuilder base, String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return base.with(jwt().jwt(j -> j.subject(SUJETO).claim("correo", "prueba@ejemplo.com"))
                .authorities(autoridades));
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

    private void denegado(MockHttpServletRequestBuilder req) throws Exception {
        mvc.perform(req).andExpect(status().isForbidden());
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder b, String cuerpo) {
        return b.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    @Test
    void confirmarAccion() throws Exception {
        String ruta = "/nexus/acciones/" + ID + "/confirmacion";
        permitido(peticion(json(post(ruta), CODIGO), "VENTAS"), "POST confirmacion", "VENTAS");
        permitido(peticion(json(post(ruta), CODIGO), "CONTADOR"), "POST confirmacion", "CONTADOR");
        denegado(peticion(json(post(ruta), CODIGO), "ADMIN"));
    }

    @Test
    void datosAlta() throws Exception {
        String ruta = "/nexus/acciones/" + ID + "/alta";
        permitido(peticion(get(ruta), "ADMIN"), "GET alta", "ADMIN");
        denegado(peticion(get(ruta), "CONTADOR"));
    }

    @Test
    void verificarAlta() throws Exception {
        String ruta = "/nexus/acciones/" + ID + "/alta/verificacion";
        permitido(peticion(json(post(ruta), CODIGO), "ADMIN"), "POST alta/verificacion", "ADMIN");
        denegado(peticion(json(post(ruta), CODIGO), "VENTAS"));
    }

    @Test
    void completarAlta() throws Exception {
        String ruta = "/nexus/acciones/" + ID + "/alta/completacion";
        permitido(peticion(json(post(ruta), CODIGO), "ADMIN"), "POST alta/completacion", "ADMIN");
        denegado(peticion(json(post(ruta), CODIGO), "CONTADOR"));
    }

    @Test
    void consultarAccion() throws Exception {
        String ruta = "/nexus/acciones/" + ID;
        permitido(peticion(get(ruta), "VENTAS"), "GET accion", "VENTAS");
        permitido(peticion(get(ruta), "CONTADOR"), "GET accion", "CONTADOR");
        permitido(peticion(get(ruta), "ADMIN"), "GET accion", "ADMIN");
        denegado(peticion(get(ruta), "DIRECCION"));
    }

    @Test
    void cancelarAccion() throws Exception {
        String ruta = "/nexus/acciones/" + ID + "/cancelacion";
        permitido(peticion(post(ruta), "VENTAS"), "POST cancelacion", "VENTAS");
        denegado(peticion(post(ruta), "ADMIN"));
    }

    @Test
    void consultarAsistente() throws Exception {
        String ruta = "/nexus/consultas";
        permitido(peticion(json(post(ruta), CONSULTA), "VENTAS"), "POST consultas", "VENTAS");
        permitido(peticion(json(post(ruta), CONSULTA), "DIRECCION"), "POST consultas", "DIRECCION");
        // Cualquier usuario autenticado puede consultar; sin token la petición no entra.
        mvc.perform(json(post(ruta), CONSULTA)).andExpect(status().isUnauthorized());
    }

    @Test
    void sugerirPoliza() throws Exception {
        String ruta = "/nexus/polizas/sugerencias";
        permitido(peticion(json(post(ruta), SUGERENCIA), "CONTADOR"), "POST sugerencias", "CONTADOR");
        denegado(peticion(json(post(ruta), SUGERENCIA), "VENTAS"));
    }
}
