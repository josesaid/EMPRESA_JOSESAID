package com.mx.josesaid.develop.ui.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class ColorBarraAdviceTest {

    private final ColorBarraAdvice advice = new ColorBarraAdvice();

    @Test
    void cadaRolTieneSuColor() {
        assertThat(advice.colorBarra(usuario("VENTAS"))).isEqualTo("azul");
        assertThat(advice.colorBarra(usuario("COMPRAS"))).isEqualTo("naranja");
        assertThat(advice.colorBarra(usuario("ALMACEN"))).isEqualTo("verde");
        assertThat(advice.colorBarra(usuario("FINANZAS"))).isEqualTo("rosa");
    }

    @Test
    void conVariosRolesGanaElDeMayorPrioridad() {
        assertThat(advice.colorBarra(usuario("VENTAS", "ADMIN"))).isEqualTo("morado");
    }

    @Test
    void sinRolConocidoOSinSesionUsaElColorPorDefecto() {
        assertThat(advice.colorBarra(usuario("DESCONOCIDO"))).isEqualTo(ColorBarraAdvice.POR_DEFECTO);
        assertThat(advice.colorBarra(null)).isEqualTo(ColorBarraAdvice.POR_DEFECTO);
    }

    private static Authentication usuario(String... roles) {
        List<SimpleGrantedAuthority> autoridades = Arrays.stream(roles)
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r)).toList();
        return new UsernamePasswordAuthenticationToken("ana@empresa.com", "x", autoridades);
    }
}
