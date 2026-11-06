package com.mx.josesaid.develop.documentos.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mx.josesaid.develop.documentos.application.DocumentosService;
import com.mx.josesaid.develop.documentos.config.SeguridadConfig;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Matriz de autorización de DocumentosControlador.
 * Un rol permitido no debe recibir 401 ni 403; un rol no permitido debe recibir 403.
 * Las escrituras usan cuerpos y partes multipart válidos.
 */
@WebMvcTest(DocumentosControlador.class)
@Import({SeguridadConfig.class, DocumentosControladorAutorizacionTest.Configuracion.class})
class DocumentosControladorAutorizacionTest {

    private static final String SUJETO = "6f1c2e7a-9a3b-4c1d-8e2f-1a2b3c4d5e6f";
    private static final String ID = "11111111-2222-3333-4444-555555555555";
    private static final String DATOS = "{\"tipo\":\"CONTRATO\",\"titulo\":\"Contrato marco\",\"area\":\"Jurídico\",\"venceEn\":null}";

    @TestConfiguration
    @EnableWebSecurity
    static class Configuracion {
    }

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private DocumentosService servicio;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @SuppressWarnings("unchecked")
    private <T extends MockHttpServletRequestBuilder> T peticion(T base, String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return (T) base.with(jwt().jwt(j -> j.subject(SUJETO)).authorities(autoridades));
    }

    private MockHttpServletRequestBuilder lectura(String ruta, String rol) {
        return peticion(get(ruta), rol);
    }

    private MockHttpServletRequestBuilder json(String ruta, String cuerpo, String rol) {
        var base = post(ruta).contentType(MediaType.APPLICATION_JSON);
        if (cuerpo != null) {
            base = base.content(cuerpo);
        }
        return peticion(base, rol);
    }

    private org.springframework.test.web.servlet.RequestBuilder crear(String rol) {
        var datos = new MockMultipartFile("datos", "datos.json", MediaType.APPLICATION_JSON_VALUE, DATOS.getBytes());
        var archivo = new MockMultipartFile("archivo", "contrato.pdf", "application/pdf", new byte[]{1, 2, 3});
        return multipart("/v1/documentos").file(datos).file(archivo).with(autenticado(rol));
    }

    private org.springframework.test.web.servlet.RequestBuilder version(String rol) {
        var archivo = new MockMultipartFile("archivo", "v2.pdf", "application/pdf", new byte[]{4, 5, 6});
        return multipart("/v1/documentos/" + ID + "/versiones").file(archivo).with(autenticado(rol));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor autenticado(String rol) {
        List<GrantedAuthority> autoridades = rol.isEmpty() ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + rol));
        return jwt().jwt(j -> j.subject(SUJETO)).authorities(autoridades);
    }

    private void permitido(org.springframework.test.web.servlet.RequestBuilder req, String descripcion, String rol) throws Exception {
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

    private void denegado(org.springframework.test.web.servlet.RequestBuilder req) throws Exception {
        mvc.perform(req).andExpect(status().isForbidden());
    }

    @Test
    void crearDocumento() throws Exception {
        permitido(crear("LEGAL"), "POST /v1/documentos", "LEGAL");
        denegado(crear("DIRECCION"));
        denegado(crear("VENTAS"));
    }

    @Test
    void agregarVersion() throws Exception {
        permitido(version("LEGAL"), "POST versiones", "LEGAL");
        denegado(version("DIRECCION"));
    }

    @Test
    void obtenerDocumento() throws Exception {
        String ruta = "/v1/documentos/" + ID;
        permitido(lectura(ruta, "LEGAL"), "GET documento", "LEGAL");
        permitido(lectura(ruta, "DIRECCION"), "GET documento", "DIRECCION");
        denegado(lectura(ruta, "VENTAS"));
    }

    @Test
    void descargarVersion() throws Exception {
        String ruta = "/v1/documentos/" + ID + "/versiones/1/descarga";
        permitido(lectura(ruta, "LEGAL"), "GET descarga", "LEGAL");
        denegado(lectura(ruta, "DIRECCION"));
    }

    @Test
    void buscarPorTitulo() throws Exception {
        String ruta = "/v1/documentos?titulo=contrato";
        permitido(lectura(ruta, "LEGAL"), "GET buscar", "LEGAL");
        permitido(lectura(ruta, "DIRECCION"), "GET buscar", "DIRECCION");
        denegado(lectura(ruta, "VENTAS"));
    }

    @Test
    void porVencer() throws Exception {
        String ruta = "/v1/documentos/por-vencer?dias=30";
        permitido(lectura(ruta, "LEGAL"), "GET por-vencer", "LEGAL");
        permitido(lectura(ruta, "DIRECCION"), "GET por-vencer", "DIRECCION");
        denegado(lectura(ruta, "VENTAS"));
    }

    @Test
    void eliminarDocumento() throws Exception {
        String ruta = "/v1/documentos/" + ID + "/eliminacion";
        String cuerpo = "{\"motivo\":\"Vencido\"}";
        permitido(json(ruta, cuerpo, "LEGAL"), "POST eliminacion", "LEGAL");
        denegado(json(ruta, cuerpo, "DIRECCION"));
    }

    @Test
    void purgarDocumento() throws Exception {
        String ruta = "/v1/documentos/" + ID + "/purga";
        String cuerpo = "{\"confirmacion\":\"" + ID + "\"}";
        permitido(json(ruta, cuerpo, "LEGAL"), "POST purga", "LEGAL");
        denegado(json(ruta, cuerpo, "DIRECCION"));
    }

    @Test
    void catalogoTiposAbiertoAAutenticados() throws Exception {
        String ruta = "/v1/catalogos/tipos-documento";
        permitido(lectura(ruta, "VENTAS"), "GET catálogo", "VENTAS");
        permitido(lectura(ruta, "LEGAL"), "GET catálogo", "LEGAL");
    }

    @Test
    void sinRolNoHayAccesoAProtegidos() throws Exception {
        denegado(lectura("/v1/documentos/" + ID, ""));
    }
}
