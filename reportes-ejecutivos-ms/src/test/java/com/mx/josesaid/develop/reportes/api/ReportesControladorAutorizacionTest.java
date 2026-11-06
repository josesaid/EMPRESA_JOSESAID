package com.mx.josesaid.develop.reportes.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.reportes.application.ReportesService;
import com.mx.josesaid.develop.reportes.config.SeguridadConfig;
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
 * Matriz de autorización de ReportesControlador.
 * JEFE_AREA lleva el claim "area" porque el controlador lo usa para acotar indicadores.
 */
@WebMvcTest(ReportesControlador.class)
@Import({SeguridadConfig.class, ReportesControladorAutorizacionTest.Configuracion.class})
class ReportesControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String INDICADOR = "{\"area\":\"FINANZAS_CONTABILIDAD\",\"nombre\":\"Ventas\","
            + "\"periodo\":\"2026-10\",\"valor\":100,\"fuenteActualizadaUtc\":\"2026-11-01T00:00:00Z\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ReportesService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockHttpServletRequestBuilder peticion(MockHttpServletRequestBuilder base, String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return base.with(jwt().jwt(j -> j.subject(SUJETO).claim("area", "VENTAS_ATENCION_CLIENTES"))
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

    @Test
    void registrarIndicador() throws Exception {
        MockHttpServletRequestBuilder req = post("/v1/indicadores").contentType(MediaType.APPLICATION_JSON).content(INDICADOR);
        permitido(peticion(req, "DIRECCION"), "POST indicadores", "DIRECCION");
        denegado(peticion(post("/v1/indicadores").contentType(MediaType.APPLICATION_JSON).content(INDICADOR), "JEFE_AREA"));
    }

    @Test
    void consultarIndicadores() throws Exception {
        String ruta = "/v1/indicadores?periodo=2026-10";
        permitido(peticion(get(ruta), "DIRECCION"), "GET indicadores", "DIRECCION");
        permitido(peticion(get(ruta), "JEFE_AREA"), "GET indicadores", "JEFE_AREA");
        denegado(peticion(get(ruta), "FINANZAS"));
    }

    @Test
    void consultarReporte() throws Exception {
        String ruta = "/v1/reportes/2026-10";
        permitido(peticion(get(ruta), "DIRECCION"), "GET reporte", "DIRECCION");
        permitido(peticion(get(ruta), "JEFE_AREA"), "GET reporte", "JEFE_AREA");
        denegado(peticion(get(ruta), "VENTAS"));
    }

    @Test
    void cerrarReporte() throws Exception {
        String ruta = "/v1/reportes/2026-10/cierre";
        permitido(peticion(post(ruta), "DIRECCION"), "POST cierre", "DIRECCION");
        denegado(peticion(post(ruta), "JEFE_AREA"));
    }

    @Test
    void exportarReporte() throws Exception {
        String ruta = "/v1/reportes/" + ID + "/exportacion";
        permitido(peticion(get(ruta), "DIRECCION"), "GET exportacion", "DIRECCION");
        denegado(peticion(get(ruta), "JEFE_AREA"));
    }

    @Test
    void sinRolNoHayAcceso() throws Exception {
        denegado(peticion(get("/v1/reportes/2026-10"), ""));
    }
}
