package com.mx.josesaid.develop.pedidos.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.pedidos.application.PedidosService;
import com.mx.josesaid.develop.pedidos.config.SeguridadConfig;
import com.mx.josesaid.develop.pedidos.infrastructure.idempotencia.Idempotencia;
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
 * Matriz de autorización de PedidosControlador.
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 */
@WebMvcTest(PedidosControlador.class)
@Import({SeguridadConfig.class, PedidosControladorAutorizacionTest.Configuracion.class})
class PedidosControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String LINEA = "{\"productoId\":\"" + ID + "\",\"descripcion\":\"Producto\",\"cantidad\":1,\"precioUnitario\":10}";
    private static final String COTIZACION = "{\"clienteId\":\"" + ID + "\",\"canal\":\"WEB\",\"moneda\":\"MXN\","
            + "\"tasaIva\":\"IVA_GENERAL\",\"vigenteHasta\":\"2026-12-31\",\"lineas\":[" + LINEA + "]}";
    private static final String PEDIDO = "{\"clienteId\":\"" + ID + "\",\"canal\":\"WEB\",\"moneda\":\"MXN\","
            + "\"tasaIva\":\"IVA_GENERAL\",\"lineas\":[" + LINEA + "]}";
    private static final String LINEAS = "{\"lineas\":[" + LINEA + "]}";
    private static final String CANCELACION = "{\"motivo\":\"ERROR_CAPTURA\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private PedidosService servicio;

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
    void crearCotizacion() throws Exception {
        permitido(peticion(json(post("/v1/cotizaciones"), COTIZACION), "VENTAS"), "POST cotizaciones", "VENTAS");
        denegado(peticion(json(post("/v1/cotizaciones"), COTIZACION), "FINANZAS"));
    }

    @Test
    void obtenerCotizacion() throws Exception {
        String ruta = "/v1/cotizaciones/" + ID;
        permitido(peticion(get(ruta), "VENTAS"), "GET cotizacion", "VENTAS");
        permitido(peticion(get(ruta), "FINANZAS"), "GET cotizacion", "FINANZAS");
        denegado(peticion(get(ruta), "ALMACEN"));
    }

    @Test
    void enviarCotizacion() throws Exception {
        String ruta = "/v1/cotizaciones/" + ID + "/envio";
        permitido(peticion(post(ruta), "VENTAS"), "POST envio", "VENTAS");
        denegado(peticion(post(ruta), "FINANZAS"));
    }

    @Test
    void aceptarCotizacion() throws Exception {
        String ruta = "/v1/cotizaciones/" + ID + "/aceptacion";
        permitido(peticion(post(ruta), "VENTAS"), "POST aceptacion", "VENTAS");
        denegado(peticion(post(ruta), "ALMACEN"));
    }

    @Test
    void convertirEnPedido() throws Exception {
        String ruta = "/v1/cotizaciones/" + ID + "/pedido";
        permitido(peticion(post(ruta), "DIRECCION"), "POST cotizacion->pedido", "DIRECCION");
        denegado(peticion(post(ruta), "COMPRAS"));
    }

    @Test
    void crearPedido() throws Exception {
        permitido(peticion(json(post("/v1/pedidos"), PEDIDO), "VENTAS"), "POST pedidos", "VENTAS");
        denegado(peticion(json(post("/v1/pedidos"), PEDIDO), "ALMACEN"));
    }

    @Test
    void buscarPedidos() throws Exception {
        String ruta = "/v1/pedidos";
        permitido(peticion(get(ruta), "VENTAS"), "GET pedidos", "VENTAS");
        permitido(peticion(get(ruta), "FINANZAS"), "GET pedidos", "FINANZAS");
        denegado(peticion(get(ruta), "COMPRAS"));
    }

    @Test
    void obtenerPedido() throws Exception {
        String ruta = "/v1/pedidos/" + ID;
        permitido(peticion(get(ruta), "VENTAS"), "GET pedido", "VENTAS");
        permitido(peticion(get(ruta), "ALMACEN"), "GET pedido", "ALMACEN");
        denegado(peticion(get(ruta), "CALIDAD"));
    }

    @Test
    void reemplazarLineas() throws Exception {
        String ruta = "/v1/pedidos/" + ID + "/lineas";
        permitido(peticion(json(put(ruta), LINEAS), "VENTAS"), "PUT lineas", "VENTAS");
        denegado(peticion(json(put(ruta), LINEAS), "ALMACEN"));
    }

    @Test
    void confirmarPedido() throws Exception {
        String ruta = "/v1/pedidos/" + ID + "/confirmacion";
        permitido(peticion(post(ruta), "VENTAS"), "POST confirmacion", "VENTAS");
        denegado(peticion(post(ruta), "ALMACEN"));
    }

    @Test
    void entregarPedido() throws Exception {
        String ruta = "/v1/pedidos/" + ID + "/entrega";
        permitido(peticion(post(ruta), "ALMACEN"), "POST entrega", "ALMACEN");
        denegado(peticion(post(ruta), "VENTAS"));
    }

    @Test
    void cancelarPedido() throws Exception {
        String ruta = "/v1/pedidos/" + ID + "/cancelacion";
        permitido(peticion(json(post(ruta), CANCELACION), "VENTAS"), "POST cancelacion", "VENTAS");
        denegado(peticion(json(post(ruta), CANCELACION), "FINANZAS"));
    }

    @Test
    void devolverPedido() throws Exception {
        String ruta = "/v1/pedidos/" + ID + "/devolucion";
        permitido(peticion(post(ruta), "DIRECCION"), "POST devolucion", "DIRECCION");
        denegado(peticion(post(ruta), "ALMACEN"));
    }

    @Test
    void metricasVentas() throws Exception {
        String ruta = "/v1/metricas/ventas?mes=2026-10";
        permitido(peticion(get(ruta), "FINANZAS"), "GET metricas", "FINANZAS");
        permitido(peticion(get(ruta), "DIRECCION"), "GET metricas", "DIRECCION");
        denegado(peticion(get(ruta), "VENTAS"));
    }

    @Test
    void catalogosAutenticados() throws Exception {
        permitido(peticion(get("/v1/catalogos/tasas-iva"), "COMPRAS"), "GET tasas-iva", "COMPRAS");
        permitido(peticion(get("/v1/catalogos/monedas"), "CALIDAD"), "GET monedas", "CALIDAD");
    }

    @Test
    void sinRolNoHayAccesoAOperaciones() throws Exception {
        denegado(peticion(get("/v1/pedidos"), ""));
        denegado(peticion(get("/v1/metricas/ventas?mes=2026-10"), ""));
    }
}
