package com.mx.josesaid.develop.usuarios_accesos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UsuarioListarRecientesTest {

    @Test
    void pideHastaQuinceUsuarios() {
        UsuarioRepositorio repositorio = mock(UsuarioRepositorio.class);
        Usuario usuario = mock(Usuario.class);
        when(repositorio.listarRecientes(eq(15))).thenReturn(List.of(usuario));
        UsuarioService servicio = new UsuarioService(repositorio, new BCryptPasswordEncoder(),
                mock(PublicadorEventos.class), Clock.systemUTC(), new SimpleMeterRegistry());

        assertThat(servicio.listarRecientes()).containsExactly(usuario);
        verify(repositorio).listarRecientes(15);
    }
}
