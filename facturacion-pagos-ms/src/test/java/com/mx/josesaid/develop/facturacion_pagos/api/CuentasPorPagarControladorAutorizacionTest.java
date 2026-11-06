package com.mx.josesaid.develop.facturacion_pagos.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.facturacion_pagos.application.CuentasPorPagarService;
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
 * Matriz de autorización de CuentasPorPagarControlador.
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 */
@WebMvcTest(CuentasPorPagarControlador.class)
@Import({SeguridadConfig.class, CuentasPorPagarControladorAutorizacionTest.Configuracion.class})
class CuentasPorPagarControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String PAGO = "{\"monto\":100,\"fechaPago\":\"2026-11-05\",\"referencia\":\"REF-1\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private CuentasPorPagarService servicio;

    @MockitoBean
    private Idempotencia idempotencia;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockHttpServletRequestBuilder peticion(MockHttpServletRequestBuilder base, String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return base.with(jwt().jwt(j -> j.subject(SUJETO)).authorities(autoridades));
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
    void listarPagos() throws Exception {
        String ruta = "/v1/cuentas-por-pagar/pagos";
        permitido(peticion(get(ruta), "FINANZAS"), "GET pagos", "FINANZAS");
        denegado(peticion(get(ruta), "VENTAS"));
    }

    @Test
    void obtenerCuenta() throws Exception {
        String ruta = "/v1/cuentas-por-pagar/" + ID;
        permitido(peticion(get(ruta), "FINANZAS"), "GET cuenta", "FINANZAS");
        denegado(peticion(get(ruta), "VENTAS"));
    }

    @Test
    void registrarPagoProveedor() throws Exception {
        String ruta = "/v1/cuentas-por-pagar/" + ID + "/pagos";
        permitido(peticion(post(ruta).contentType(MediaType.APPLICATION_JSON).content(PAGO), "FINANZAS"),
                "POST pagos", "FINANZAS");
        denegado(peticion(post(ruta).contentType(MediaType.APPLICATION_JSON).content(PAGO), "VENTAS"));
    }

    @Test
    void sinRolNoHayAcceso() throws Exception {
        denegado(peticion(get("/v1/cuentas-por-pagar/" + ID), ""));
    }
}
