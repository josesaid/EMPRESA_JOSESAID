package com.mx.josesaid.develop.ui.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CorreoAdviceTest {

    private final CorreoAdvice advice = new CorreoAdvice();

    @Test
    void usuarioLogueadoVeSuCorreo() {
        var usuario = new UsernamePasswordAuthenticationToken("almacen@empresa.mx", "x",
                List.of(new SimpleGrantedAuthority("ROLE_ALMACEN")));

        assertThat(advice.correo(usuario)).isEqualTo("almacen@empresa.mx");
    }

    @Test
    void sinSesionOAnonimoNoHayCorreo() {
        var anonimo = new AnonymousAuthenticationToken("clave", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));

        assertThat(advice.correo(null)).isNull();
        assertThat(advice.correo(anonimo)).isNull();
    }
}
