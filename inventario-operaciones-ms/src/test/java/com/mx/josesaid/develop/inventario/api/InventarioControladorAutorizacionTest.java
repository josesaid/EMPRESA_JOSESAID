package com.mx.josesaid.develop.inventario.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.inventario.application.InventarioService;
import com.mx.josesaid.develop.inventario.config.SeguridadConfig;
import com.mx.josesaid.develop.inventario.infrastructure.idempotencia.Idempotencia;
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
 * Matriz de autorización de InventarioControlador.
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 */
@WebMvcTest(InventarioControlador.class)
@Import({SeguridadConfig.class, InventarioControladorAutorizacionTest.Configuracion.class})
class InventarioControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String ARTICULO = "{\"sku\":\"SKU-1\",\"nombre\":\"Tornillo\",\"unidad\":\"pza\",\"loteObligatorio\":false,\"minimo\":10}";
    private static final String ALMACEN = "{\"nombre\":\"Central\",\"ubicacion\":\"Zapopan\"}";
    private static final String MOVIMIENTO = "{\"articuloId\":\"" + ID + "\",\"almacenId\":\"" + ID + "\",\"tipo\":\"ENTRADA\",\"cantidad\":5}";
    private static final String ORDEN = "{\"tipo\":\"PRODUCCION\",\"cantidadObjetivo\":10}";
    private static final String CIERRE = "{\"cantidadReal\":9}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private InventarioService servicio;

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
    void crearArticulo() throws Exception {
        permitido(peticion(json(post("/v1/articulos"), ARTICULO), "OPERACIONES"), "POST articulos", "OPERACIONES");
        denegado(peticion(json(post("/v1/articulos"), ARTICULO), "ALMACEN"));
    }

    @Test
    void crearAlmacen() throws Exception {
        permitido(peticion(json(post("/v1/almacenes"), ALMACEN), "ALMACEN"), "POST almacenes", "ALMACEN");
        denegado(peticion(json(post("/v1/almacenes"), ALMACEN), "OPERACIONES"));
    }

    @Test
    void registrarMovimiento() throws Exception {
        permitido(peticion(json(post("/v1/movimientos"), MOVIMIENTO), "ALMACEN"), "POST movimientos", "ALMACEN");
        denegado(peticion(json(post("/v1/movimientos"), MOVIMIENTO), "COMPRAS"));
    }

    @Test
    void articulosBajoMinimo() throws Exception {
        String ruta = "/v1/articulos/bajo-minimo";
        permitido(peticion(get(ruta), "ALMACEN"), "GET bajo-minimo", "ALMACEN");
        permitido(peticion(get(ruta), "COMPRAS"), "GET bajo-minimo", "COMPRAS");
        denegado(peticion(get(ruta), "VENTAS"));
    }

    @Test
    void movimientosDeArticulo() throws Exception {
        String ruta = "/v1/movimientos?articulo=" + ID;
        permitido(peticion(get(ruta), "ALMACEN"), "GET movimientos", "ALMACEN");
        permitido(peticion(get(ruta), "COMPRAS"), "GET movimientos", "COMPRAS");
        denegado(peticion(get(ruta), "VENTAS"));
    }

    @Test
    void existencias() throws Exception {
        String ruta = "/v1/existencias?articulo=" + ID;
        permitido(peticion(get(ruta), "ALMACEN"), "GET existencias", "ALMACEN");
        permitido(peticion(get(ruta), "COMPRAS"), "GET existencias", "COMPRAS");
        permitido(peticion(get(ruta), "VENTAS"), "GET existencias", "VENTAS");
        denegado(peticion(get(ruta), "CALIDAD"));
    }

    @Test
    void crearOrdenOperacion() throws Exception {
        permitido(peticion(json(post("/v1/ordenes-operacion"), ORDEN), "OPERACIONES"), "POST ordenes", "OPERACIONES");
        denegado(peticion(json(post("/v1/ordenes-operacion"), ORDEN), "CALIDAD"));
    }

    @Test
    void cerrarOrdenOperacion() throws Exception {
        String ruta = "/v1/ordenes-operacion/" + ID + "/cierre";
        permitido(peticion(json(post(ruta), CIERRE), "OPERACIONES"), "POST cierre", "OPERACIONES");
        denegado(peticion(json(post(ruta), CIERRE), "ALMACEN"));
    }

    @Test
    void trazabilidadDeLote() throws Exception {
        String ruta = "/v1/lotes/LOTE-1/trazabilidad";
        permitido(peticion(get(ruta), "OPERACIONES"), "GET trazabilidad", "OPERACIONES");
        permitido(peticion(get(ruta), "CALIDAD"), "GET trazabilidad", "CALIDAD");
        denegado(peticion(get(ruta), "COMPRAS"));
    }

    @Test
    void sinRolNoHayAcceso() throws Exception {
        denegado(peticion(get("/v1/articulos/bajo-minimo"), ""));
    }
}
