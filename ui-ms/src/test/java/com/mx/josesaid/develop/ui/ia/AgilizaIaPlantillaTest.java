package com.mx.josesaid.develop.ui.ia;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.ui.ia.SugerenciasIa.Opcion;
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

class AgilizaIaPlantillaTest {

    private final SpringTemplateEngine motor = motor();

    @Test
    void muestraLasOpcionesYLosPasosDeLaElegida() {
        Opcion elegida = SugerenciasIa.opcion(List.of("ALMACEN"), "ALMACEN-0").orElseThrow();
        WebContext ctx = contexto();
        ctx.setVariable("opciones", SugerenciasIa.opciones(List.of("ALMACEN")));
        ctx.setVariable("elegida", elegida);

        String html = motor.process("agiliza/ia", ctx);

        assertThat(html).contains("Agiliza con IA", elegida.texto(), "Consultar");
        assertThat(html).contains("/agiliza-ia?opcion=ALMACEN-1");
    }

    @Test
    void sinElegidaNoHayPasos() {
        WebContext ctx = contexto();
        ctx.setVariable("opciones", SugerenciasIa.opciones(List.of("ALMACEN")));
        ctx.setVariable("elegida", null);

        String html = motor.process("agiliza/ia", ctx);

        assertThat(html).contains("Elige una opción");
        assertThat(html).doesNotContain("<h2 class=\"h6\"");
    }

    private static WebContext contexto() {
        MockServletContext servlet = new MockServletContext();
        JakartaServletWebApplication aplicacion = JakartaServletWebApplication.buildApplication(servlet);
        WebContext ctx = new WebContext(aplicacion.buildExchange(new MockHttpServletRequest(servlet),
                new MockHttpServletResponse()), Locale.forLanguageTag("es-MX"));
        ctx.setVariable("titulo", "Agiliza con IA");
        ctx.setVariable("correo", "ana@empresa.mx");
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
