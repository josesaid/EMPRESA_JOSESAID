package com.mx.josesaid.develop.ui.ia;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.ui.ia.SugerenciasIa.Sugerencia;
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

/** Verifica que el panel de sugerencias aparezca en el layout solo cuando hay sesión. */
class PanelSugerenciasPlantillaTest {

    private final SpringTemplateEngine motor = motor();

    @Test
    void panelMuestraLasSugerenciasDelLog() {
        WebContext ctx = contexto();
        ctx.setVariable("titulo", "Nuevo proveedor");
        ctx.setVariable("correo", "ana@empresa.mx");
        ctx.setVariable("sugerenciasIa", List.of(new Sugerencia("09:05", "VENTAS", "Resumir el historial de interacciones.", List.of("Obtener interacciones.", "Resumir."))));

        String html = motor.process("altas/proveedor", ctx);

        assertThat(html).contains("Sugerencias de IA", "Resumir el historial de interacciones.", "09:05", "VENTAS", "Obtener interacciones.");
    }

    @Test
    void sinSesionNoHayPanel() {
        WebContext ctx = contexto();
        ctx.setVariable("titulo", "Nuevo proveedor");
        ctx.setVariable("correo", "");

        String html = motor.process("altas/proveedor", ctx);

        assertThat(html).doesNotContain("Sugerencias de IA");
    }

    private static WebContext contexto() {
        MockServletContext servlet = new MockServletContext();
        JakartaServletWebApplication aplicacion = JakartaServletWebApplication.buildApplication(servlet);
        return new WebContext(aplicacion.buildExchange(new MockHttpServletRequest(servlet), new MockHttpServletResponse()), Locale.forLanguageTag("es-MX"));
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
