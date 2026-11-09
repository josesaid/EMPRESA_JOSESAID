package com.mx.josesaid.develop.ui.altas;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

/** Renderiza las plantillas de alta para detectar errores de sintaxis Thymeleaf sin levantar la aplicación. */
class AltasPlantillasTest {

    private final SpringTemplateEngine motor = motor();

    @Test
    void formularioDeClienteRendereaRolesYCampos() {
        WebContext ctx = contexto();
        ctx.setVariable("titulo", "Nuevo cliente");
        ctx.setVariable("roles", List.of("COMPRAS", "PAGOS", "ASISTENCIA"));

        String html = motor.process("altas/cliente", ctx);

        assertThat(html).contains("name=\"rfc\"", "name=\"contactoRol\"", "value=\"PAGOS\"", "Guardar cliente");
    }

    @Test
    void formularioDeProveedorRendereaCamposYError() {
        WebContext ctx = contexto();
        ctx.setVariable("titulo", "Nuevo proveedor");
        ctx.setVariable("error", "Ya existe un registro con ese RFC o correo. No se creó nada.");
        ctx.setVariable("valores", Map.of("razonSocial", "Proveedor Prueba SA"));

        String html = motor.process("altas/proveedor", ctx);

        assertThat(html).contains("Proveedor Prueba SA", "Ya existe un registro", "name=\"correo\"");
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
