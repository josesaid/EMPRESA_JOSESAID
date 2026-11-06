package com.mx.josesaid.develop.contabilidad.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.contabilidad.application.ContabilidadService;
import com.mx.josesaid.develop.contabilidad.config.SeguridadConfig;
import com.mx.josesaid.develop.contabilidad.infrastructure.idempotencia.Idempotencia;
import java.util.List;
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
 * Matriz de autorización de ContabilidadControlador.
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 * Los cuerpos de las escrituras son válidos: así la validación no oculta la autorización.
 */
@WebMvcTest(ContabilidadControlador.class)
@Import({SeguridadConfig.class, ContabilidadControladorAutorizacionTest.Configuracion.class})
class ContabilidadControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String CUENTA = "{\"codigo\":\"1101\",\"nombre\":\"Caja\",\"naturaleza\":\"DEUDORA\"}";
    private static final String POLIZA = "{\"tipo\":\"DIARIO\",\"fecha\":\"2026-11-05\",\"movimientos\":["
            + "{\"cuentaId\":\"" + ID + "\",\"cargo\":100,\"abono\":null},"
            + "{\"cuentaId\":\"" + ID + "\",\"cargo\":null,\"abono\":100}]}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ContabilidadService servicio;

    @MockitoBean
    private Idempotencia idempotencia;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockHttpServletRequestBuilder peticion(MockHttpServletRequestBuilder base, String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return base.with(jwt().jwt(j -> j.subject(SUJETO)).authorities(autoridades));
    }

    private MockHttpServletRequestBuilder lectura(String ruta, String rol) {
        return peticion(get(ruta), rol);
    }

    private MockHttpServletRequestBuilder escritura(String ruta, String cuerpo, String rol) {
        var base = post(ruta).contentType(MediaType.APPLICATION_JSON);
        if (cuerpo != null) {
            base = base.content(cuerpo);
        }
        return peticion(base, rol);
    }

    private void permitido(MockHttpServletRequestBuilder req, String descripcion, String rol) throws Exception {
        int codigo;
        try {
            codigo = mvc.perform(req).andReturn().getResponse().getStatus();
        } catch (jakarta.servlet.ServletException e) {
            codigo = 200; // llegó al controlador: el rol fue aceptado
        }
        org.assertj.core.api.Assertions.assertThat(codigo)
                .as("%s debería estar permitido para %s", descripcion, rol)
                .isNotIn(401, 403);
    }

    private void denegado(MockHttpServletRequestBuilder req) throws Exception {
        mvc.perform(req).andExpect(status().isForbidden());
    }

    @Test
    void listarCuentas() throws Exception {
        permitido(lectura("/v1/cuentas", "CONTADOR"), "GET /v1/cuentas", "CONTADOR");
        permitido(lectura("/v1/cuentas", "DIRECCION"), "GET /v1/cuentas", "DIRECCION");
        denegado(lectura("/v1/cuentas", "VENTAS"));
    }

    @Test
    void crearCuenta() throws Exception {
        permitido(escritura("/v1/cuentas", CUENTA, "CONTADOR"), "POST /v1/cuentas", "CONTADOR");
        denegado(escritura("/v1/cuentas", CUENTA, "DIRECCION"));
        denegado(escritura("/v1/cuentas", CUENTA, "VENTAS"));
    }

    @Test
    void listarPolizas() throws Exception {
        String ruta = "/v1/polizas?periodo=2026-11";
        permitido(lectura(ruta, "CONTADOR"), "GET /v1/polizas", "CONTADOR");
        permitido(lectura(ruta, "DIRECCION"), "GET /v1/polizas", "DIRECCION");
        denegado(lectura(ruta, "VENTAS"));
    }

    @Test
    void obtenerPoliza() throws Exception {
        String ruta = "/v1/polizas/" + ID;
        permitido(lectura(ruta, "CONTADOR"), "GET /v1/polizas/{id}", "CONTADOR");
        permitido(lectura(ruta, "DIRECCION"), "GET /v1/polizas/{id}", "DIRECCION");
        denegado(lectura(ruta, "VENTAS"));
    }

    @Test
    void registrarPoliza() throws Exception {
        permitido(escritura("/v1/polizas", POLIZA, "CONTADOR"), "POST /v1/polizas", "CONTADOR");
        denegado(escritura("/v1/polizas", POLIZA, "DIRECCION"));
        denegado(escritura("/v1/polizas", POLIZA, "VENTAS"));
    }

    @Test
    void reversarPoliza() throws Exception {
        String ruta = "/v1/polizas/" + ID + "/reversa";
        permitido(escritura(ruta, null, "CONTADOR"), "POST reversa", "CONTADOR");
        denegado(escritura(ruta, null, "DIRECCION"));
    }

    @Test
    void cerrarPeriodo() throws Exception {
        String ruta = "/v1/periodos/2026-11/cierre";
        permitido(escritura(ruta, null, "CONTADOR"), "POST cierre", "CONTADOR");
        denegado(escritura(ruta, null, "DIRECCION"));
    }

    @Test
    void estadosFinancieros() throws Exception {
        String ruta = "/v1/estados-financieros?periodo=2026-11";
        permitido(lectura(ruta, "CONTADOR"), "GET estados-financieros", "CONTADOR");
        permitido(lectura(ruta, "DIRECCION"), "GET estados-financieros", "DIRECCION");
        denegado(lectura(ruta, "VENTAS"));
    }

    @Test
    void sinRolNoHayAcceso() throws Exception {
        denegado(lectura("/v1/cuentas", ""));
    }
}
