package com.mx.josesaid.develop.facturacion_pagos.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.facturacion_pagos.application.FacturacionService;
import com.mx.josesaid.develop.facturacion_pagos.config.SeguridadConfig;
import com.mx.josesaid.develop.facturacion_pagos.infrastructure.idempotencia.Idempotencia;
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
 * Matriz de autorización de ComprobanteControlador.
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 */
@WebMvcTest(ComprobanteControlador.class)
@Import({SeguridadConfig.class, ComprobanteControladorAutorizacionTest.Configuracion.class})
class ComprobanteControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String CREAR = "{\"ordenId\":\"" + ID + "\",\"moneda\":\"MXN\",\"subtotal\":100}";
    private static final String CANCELAR = "{\"motivo\":\"Error en el monto\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FacturacionService servicio;

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
    void crearComprobante() throws Exception {
        permitido(escritura("/v1/comprobantes", CREAR, "FINANZAS"), "POST comprobantes", "FINANZAS");
        denegado(escritura("/v1/comprobantes", CREAR, "VENTAS"));
    }

    @Test
    void buscarPorFolio() throws Exception {
        String ruta = "/v1/comprobantes?folio=A-1";
        permitido(lectura(ruta, "FINANZAS"), "GET folio", "FINANZAS");
        permitido(lectura(ruta, "VENTAS"), "GET folio", "VENTAS");
        denegado(lectura(ruta, "LEGAL"));
    }

    @Test
    void listarPendientesCobro() throws Exception {
        String ruta = "/v1/comprobantes/pendientes-cobro";
        permitido(lectura(ruta, "FINANZAS"), "GET pendientes", "FINANZAS");
        permitido(lectura(ruta, "VENTAS"), "GET pendientes", "VENTAS");
        denegado(lectura(ruta, "LEGAL"));
    }

    @Test
    void obtenerComprobante() throws Exception {
        String ruta = "/v1/comprobantes/" + ID;
        permitido(lectura(ruta, "FINANZAS"), "GET comprobante", "FINANZAS");
        permitido(lectura(ruta, "VENTAS"), "GET comprobante", "VENTAS");
        denegado(lectura(ruta, "LEGAL"));
    }

    @Test
    void emitirComprobante() throws Exception {
        String ruta = "/v1/comprobantes/" + ID + "/emision";
        permitido(escritura(ruta, null, "FINANZAS"), "POST emision", "FINANZAS");
        denegado(escritura(ruta, null, "VENTAS"));
    }

    @Test
    void cancelarComprobante() throws Exception {
        String ruta = "/v1/comprobantes/" + ID + "/cancelacion";
        permitido(escritura(ruta, CANCELAR, "FINANZAS"), "POST cancelacion", "FINANZAS");
        denegado(escritura(ruta, CANCELAR, "VENTAS"));
    }

    @Test
    void registrarPagoSobreComprobante() throws Exception {
        String ruta = "/v1/comprobantes/" + ID + "/pagos";
        String pago = "{\"monto\":100,\"fechaPago\":\"2026-11-05\",\"referencia\":\"REF-1\"}";
        permitido(escritura(ruta, pago, "FINANZAS"), "POST pagos", "FINANZAS");
        denegado(escritura(ruta, pago, "VENTAS"));
    }

    @Test
    void sinRolNoHayAcceso() throws Exception {
        denegado(lectura("/v1/comprobantes/" + ID, ""));
    }
}
