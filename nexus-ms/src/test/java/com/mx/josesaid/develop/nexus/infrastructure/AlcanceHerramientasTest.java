package com.mx.josesaid.develop.nexus.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.RestClient;

class AlcanceHerramientasTest {

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void comoRol(String rol) {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken("u", null, rol));
    }

    @Test
    void cadaHerramientaRegistradaTieneRolesDefinidos() {
        RestClient sinRed = RestClient.builder().build();
        var herramientas = ToolCallbacks.from(new HerramientasReportes(sinRed), new HerramientasClientes(sinRed),
                new HerramientasVentas(sinRed), new HerramientasCompras(sinRed), new HerramientasFinanzas(sinRed),
                new HerramientasContabilidad(sinRed), new HerramientasInventario(sinRed),
                new HerramientasDocumentos(sinRed), new HerramientasUsuarios(sinRed));

        assertThat(herramientas).isNotEmpty();
        for (var h : herramientas) {
            assertThat(AlcanceHerramientas.conoce(h.getToolDefinition().name()))
                    .as("sin roles definidos: %s", h.getToolDefinition().name())
                    .isTrue();
        }
    }

    @Test
    void sinAutenticacionNoPuedeUsarNada() {
        assertThat(AlcanceHerramientas.puedeUsar("consultar_pedidos")).isFalse();
    }

    @Test
    void compras_puedeOrdenPeroNoTrazabilidadDeLotes() {
        comoRol("ROLE_COMPRAS");

        assertThat(AlcanceHerramientas.puedeUsar("consultar_orden_compra")).isTrue();
        assertThat(AlcanceHerramientas.puedeUsar("consultar_trazabilidad_lote")).isFalse();
    }

    @Test
    void calidad_puedeTrazabilidadPeroNoUsuarios() {
        comoRol("ROLE_CALIDAD");

        assertThat(AlcanceHerramientas.puedeUsar("consultar_trazabilidad_lote")).isTrue();
        assertThat(AlcanceHerramientas.puedeUsar("consultar_usuario")).isFalse();
    }

    @Test
    void ventas_noPuedeResumenDeVentas() {
        comoRol("ROLE_VENTAS");

        assertThat(AlcanceHerramientas.puedeUsar("consultar_pedidos")).isTrue();
        assertThat(AlcanceHerramientas.puedeUsar("resumen_ventas")).isFalse();
    }

    @Test
    void finanzas_puedeResumenDeVentas() {
        comoRol("ROLE_FINANZAS");

        assertThat(AlcanceHerramientas.puedeUsar("resumen_ventas")).isTrue();
    }

    @Test
    void herramientaDesconocidaSeNiega() {
        comoRol("ROLE_ADMIN");

        assertThat(AlcanceHerramientas.puedeUsar("herramienta_inexistente")).isFalse();
    }
}
