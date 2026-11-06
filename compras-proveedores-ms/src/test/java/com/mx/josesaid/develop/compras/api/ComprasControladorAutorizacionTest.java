package com.mx.josesaid.develop.compras.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.compras.application.ComprasService;
import com.mx.josesaid.develop.compras.config.SeguridadConfig;
import com.mx.josesaid.develop.compras.infrastructure.idempotencia.Idempotencia;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
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
 * Matriz de autorización de ComprasControlador (endpoints de lectura).
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 */
@WebMvcTest(ComprasControlador.class)
@Import({SeguridadConfig.class, ComprasControladorAutorizacionTest.Configuracion.class})
class ComprasControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String PROVEEDOR = "{\"razonSocial\":\"Proveedor SA\",\"rfc\":\"AAA010101AAA\",\"correo\":\"p@ejemplo.com\"}";
    private static final String ORDEN = "{\"proveedorId\":\"" + ID + "\",\"moneda\":\"MXN\",\"lineas\":["
            + "{\"articuloSku\":\"SKU-1\",\"cantidad\":2,\"precioUnitario\":10}]}";
    private static final String RECEPCION = "{\"lineas\":[{\"articuloSku\":\"SKU-1\",\"cantidad\":2}]}";
    private static final String CANCELAR = "{\"motivo\":\"Proveedor sin entrega\"}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ComprasService servicio;

    @MockitoBean
    private Idempotencia idempotencia;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    private MockHttpServletRequestBuilder lectura(String ruta, String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return get(ruta).with(jwt().jwt(j -> j.subject(SUJETO)).authorities(autoridades));
    }

    private void permitido(String ruta, String rol) throws Exception {
        int codigo;
        try {
            codigo = mvc.perform(lectura(ruta, rol)).andReturn().getResponse().getStatus();
        } catch (jakarta.servlet.ServletException e) {
            codigo = 200; // llegó al controlador: el rol fue aceptado
        }
        org.assertj.core.api.Assertions.assertThat(codigo)
                .as("%s debería estar permitido para %s", ruta, rol)
                .isNotIn(401, 403);
    }

    private void denegado(String ruta, String rol) throws Exception {
        mvc.perform(lectura(ruta, rol)).andExpect(status().isForbidden());
    }

    @Test
    void buscarProveedores() throws Exception {
        String ruta = "/v1/proveedores?razonSocial=prueba";
        permitido(ruta, "COMPRAS");
        permitido(ruta, "FINANZAS");
        denegado(ruta, "ALMACEN");
    }

    @Test
    void obtenerProveedor() throws Exception {
        String ruta = "/v1/proveedores/" + ID;
        permitido(ruta, "COMPRAS");
        denegado(ruta, "ALMACEN");
    }

    @Test
    void listarOrdenesCompra() throws Exception {
        String ruta = "/v1/ordenes-compra";
        permitido(ruta, "COMPRAS");
        permitido(ruta, "ALMACEN");
        permitido(ruta, "DIRECCION");
        denegado(ruta, "FINANZAS");
    }

    @Test
    void obtenerOrdenCompra() throws Exception {
        String ruta = "/v1/ordenes-compra/" + ID;
        permitido(ruta, "COMPRAS");
        denegado(ruta, "FINANZAS");
    }

    @Test
    void sinRolNoHayAcceso() throws Exception {
        denegado("/v1/proveedores?razonSocial=prueba", "");
    }

    private void escribe(MockHttpServletRequestBuilder req, String ruta, String rol, String cuerpo, boolean permite)
            throws Exception {
        MockHttpServletRequestBuilder conRol = req.contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .with(jwt().jwt(j -> j.subject(SUJETO))
                        .authorities(rol.isEmpty() ? java.util.List.of()
                                : java.util.List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
        if (permite) {
            int codigo;
            try {
                codigo = mvc.perform(conRol).andReturn().getResponse().getStatus();
            } catch (jakarta.servlet.ServletException e) {
                codigo = 200; // llegó al controlador: el rol fue aceptado
            }
            org.assertj.core.api.Assertions.assertThat(codigo)
                    .as("%s debería estar permitido para %s", ruta, rol)
                    .isNotIn(401, 403);
        } else {
            mvc.perform(conRol).andExpect(status().isForbidden());
        }
    }

    @Test
    void crearProveedor() throws Exception {
        String ruta = "/v1/proveedores";
        escribe(post(ruta), ruta, "COMPRAS", PROVEEDOR, true);
        escribe(post(ruta), ruta, "FINANZAS", PROVEEDOR, false);
    }

    @Test
    void crearOrdenCompra() throws Exception {
        String ruta = "/v1/ordenes-compra";
        escribe(post(ruta), ruta, "COMPRAS", ORDEN, true);
        escribe(post(ruta), ruta, "ALMACEN", ORDEN, false);
    }

    @Test
    void aprobarOrdenCompra() throws Exception {
        String ruta = "/v1/ordenes-compra/" + ID + "/aprobacion";
        escribe(post(ruta), ruta, "COMPRAS", "", true);
        escribe(post(ruta), ruta, "DIRECCION", "", true);
        escribe(post(ruta), ruta, "ALMACEN", "", false);
    }

    @Test
    void registrarRecepcion() throws Exception {
        String ruta = "/v1/ordenes-compra/" + ID + "/recepciones";
        escribe(post(ruta), ruta, "ALMACEN", RECEPCION, true);
        escribe(post(ruta), ruta, "COMPRAS", RECEPCION, false);
    }

    @Test
    void cancelarOrdenCompra() throws Exception {
        String ruta = "/v1/ordenes-compra/" + ID + "/cancelacion";
        escribe(post(ruta), ruta, "COMPRAS", CANCELAR, true);
        escribe(post(ruta), ruta, "DIRECCION", CANCELAR, false);
    }
}
