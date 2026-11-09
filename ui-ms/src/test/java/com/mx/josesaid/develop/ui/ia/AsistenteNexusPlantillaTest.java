package com.mx.josesaid.develop.ui.ia;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.ui.web.AsistenteNexusController.Mensaje;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

class AsistenteNexusPlantillaTest {

    private final SpringTemplateEngine motor = motor();

    @Test
    void muestraElHistorialYElFormularioDePregunta() {
        WebContext ctx = contexto();
        ctx.setVariable("mensajes", List.of(new Mensaje("Tú", "¿Qué existencias hay?"), new Mensaje("Nexus", "Hay 100.")));

        String html = motor.process("asistente/nexus", ctx);

        assertThat(html).contains("Asistente Nexus", "¿Qué existencias hay?", "Hay 100.", "name=\"pregunta\"", "action=\"/asistente\"");
    }

    @Test
    void sinMensajesMuestraAyudaYSinBotonLimpiar() {
        WebContext ctx = contexto();
        ctx.setVariable("mensajes", List.of());

        String html = motor.process("asistente/nexus", ctx);

        assertThat(html).contains("Aún no hay mensajes").doesNotContain("Limpiar conversación");
    }

    private static WebContext contexto() {
        MockServletContext servlet = new MockServletContext();
        JakartaServletWebApplication aplicacion = JakartaServletWebApplication.buildApplication(servlet);
        WebContext ctx = new WebContext(aplicacion.buildExchange(new MockHttpServletRequest(servlet),
                new MockHttpServletResponse()), Locale.forLanguageTag("es-MX"));
        ctx.setVariable("titulo", "Asistente Nexus");
        ctx.setVariable("correo", "almacen@empresa.mx");
        return ctx;
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
