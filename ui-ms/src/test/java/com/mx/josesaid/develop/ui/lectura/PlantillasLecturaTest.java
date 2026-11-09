package com.mx.josesaid.develop.ui.lectura;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/** Renderiza las plantillas para detectar errores de sintaxis Thymeleaf sin levantar la aplicación. */
class PlantillasLecturaTest {

    private final SpringTemplateEngine motor = motor();

    @Test
    void tablaRendereaFilasYEnlaces() {
        WebContext ctx = contexto();
        ctx.setVariable("titulo", "Proveedores");
        ctx.setVariable("seccion", "proveedores");
        ctx.setVariable("correo", "ana@empresa.com");
        ctx.setVariable("rutaVista", "/compras/proveedores");
        ctx.setVariable("campos", List.of(Map.of("nombre", "razonSocial", "etiqueta", "Razón social", "valor", "Pap", "requerido", "true")));
        ctx.setVariable("ejecutado", true);
        ctx.setVariable("ayuda", "");
        ctx.setVariable("columnas", List.of(Map.of("nombre", "razonSocial", "etiqueta", "Razón social")));
        ctx.setVariable("filas", List.of(Map.of("razonSocial", "Papelería SA", "_enlace", "/compras/proveedores/abc")));

        String html = motor.process("lectura/tabla", ctx);

        assertThat(html).contains("Papelería SA", "href=\"/compras/proveedores/abc\"", "EMPRESA_JOSESAID", "Cerrar sesión");
    }

    @Test
    void tablaMuestraErrorSinTabla() {
        WebContext ctx = contexto();
        ctx.setVariable("titulo", "Usuarios");
        ctx.setVariable("seccion", "usuarios");
        ctx.setVariable("campos", List.of());
        ctx.setVariable("ejecutado", false);
        ctx.setVariable("error", "Tu rol no tiene acceso a esta sección.");

        String html = motor.process("lectura/tabla", ctx);

        assertThat(html).contains("Tu rol no tiene acceso a esta sección.").doesNotContain("<table");
    }

    @Test
    void detalleRendereaEntradasYSubtabla() {
        WebContext ctx = contexto();
        ctx.setVariable("titulo", "Cliente");
        ctx.setVariable("seccion", "clientes");
        ctx.setVariable("volver", "/clientes");
        ctx.setVariable("rutaVista", "/clientes");
        ctx.setVariable("campos", List.of());
        ctx.setVariable("detalle", List.of(Map.of("etiqueta", "Nombre", "valor", "Ana López")));
        ctx.setVariable("subtitulo", "Interacciones");
        ctx.setVariable("subcolumnas", List.of(Map.of("nombre", "tipo", "etiqueta", "Tipo")));
        ctx.setVariable("subfilas", List.of(Map.of("tipo", "LLAMADA")));

        String html = motor.process("lectura/detalle", ctx);

        assertThat(html).contains("Ana López", "Interacciones", "LLAMADA", "href=\"/clientes\"");
    }

    @Test
    void inicioRendereaTarjetas() {
        WebContext ctx = contexto();
        ctx.setVariable("roles", List.of("ADMIN"));
        ctx.setVariable("correo", "ana@empresa.com");

        String html = motor.process("inicio", ctx);

        assertThat(html).contains("Panel general", "/clientes", "ADMIN");
    }

    private static WebContext contexto() {
        MockServletContext servlet = new MockServletContext();
        JakartaServletWebApplication aplicacion = JakartaServletWebApplication.buildApplication(servlet);
        return new WebContext(aplicacion.buildExchange(new MockHttpServletRequest(servlet), new MockHttpServletResponse()));
    }

    private static SpringTemplateEngine motor() {
        ClassLoaderTemplateResolver resolutor = new ClassLoaderTemplateResolver();
        resolutor.setPrefix("templates/");
        resolutor.setSuffix(".html");
        resolutor.setTemplateMode(TemplateMode.HTML);
        resolutor.setCharacterEncoding("UTF-8");
        SpringTemplateEngine motor = new SpringTemplateEngine();
        motor.setTemplateResolver(resolutor);
        return motor;
    }
}
