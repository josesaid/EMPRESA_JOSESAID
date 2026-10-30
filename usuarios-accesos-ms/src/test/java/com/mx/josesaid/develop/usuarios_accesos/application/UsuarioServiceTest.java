package com.mx.josesaid.develop.usuarios_accesos.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mx.josesaid.develop.usuarios_accesos.domain.ConflictoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.CredencialesInvalidasException;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class UsuarioServiceTest {

    private final Map<UUID, Usuario> almacen = new HashMap<>();
    private final List<EventoDominio> eventos = new ArrayList<>();
    private UsuarioService servicio;

    /** Repositorio en memoria: prueba la lógica de aplicación sin base de datos. */
    private final UsuarioRepositorio repositorio = new UsuarioRepositorio() {
        @Override
        public Usuario guardar(Usuario usuario) {
            almacen.put(usuario.getId(), usuario);
            return usuario;
        }

        @Override
        public Optional<Usuario> buscarPorId(UUID id) {
            return Optional.ofNullable(almacen.get(id));
        }

        @Override
        public Optional<Usuario> buscarPorCorreo(String correo) {
            return almacen.values().stream().filter(u -> u.getCorreo().equals(correo)).findFirst();
        }

        @Override
        public boolean existePorCorreo(String correo) {
            return buscarPorCorreo(correo).isPresent();
        }
    };

    @BeforeEach
    void preparar() {
        Clock reloj = Clock.fixed(Instant.parse("2026-10-29T15:00:00Z"), ZoneOffset.UTC);
        servicio = new UsuarioService(repositorio, new BCryptPasswordEncoder(), eventos::add, reloj);
    }

    @Test
    void autenticaConCredencialesCorrectas() {
        servicio.crear("ana@empresa.mx", "Ana Ruiz", "clave-segura-1", Set.of(Rol.VENTAS));
        Usuario u = servicio.autenticar("ANA@empresa.mx", "clave-segura-1");
        assertThat(u.getCorreo()).isEqualTo("ana@empresa.mx");
        assertThat(u.getIntentosFallidos()).isZero();
    }

    @Test
    void contrasenaIncorrectaGuardaElIntentoYRechaza() {
        servicio.crear("ana@empresa.mx", "Ana Ruiz", "clave-segura-1", Set.of(Rol.VENTAS));
        assertThatThrownBy(() -> servicio.autenticar("ana@empresa.mx", "otra"))
                .isInstanceOf(CredencialesInvalidasException.class);
        Usuario guardado = almacen.values().iterator().next();
        assertThat(guardado.getIntentosFallidos()).isEqualTo(1);
    }

    @Test
    void quintoIntentoFallidoPublicaEventoDeBloqueo() {
        servicio.crear("ana@empresa.mx", "Ana Ruiz", "clave-segura-1", Set.of(Rol.VENTAS));
        for (int i = 0; i < Usuario.MAX_INTENTOS_FALLIDOS; i++) {
            assertThatThrownBy(() -> servicio.autenticar("ana@empresa.mx", "mala"))
                    .isInstanceOf(CredencialesInvalidasException.class);
        }
        assertThat(eventos).hasSize(1);
        assertThat(eventos.get(0).tipo()).isEqualTo(UsuarioService.EVENTO_USUARIO_BLOQUEADO);
    }

    @Test
    void noPermiteCorreoDuplicado() {
        servicio.crear("ana@empresa.mx", "Ana Ruiz", "clave-segura-1", Set.of(Rol.VENTAS));
        assertThatThrownBy(() -> servicio.crear("ANA@empresa.mx", "Otra", "clave", Set.of(Rol.VENTAS)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void cambiarRolesPublicaEventoConRolesAnterioresYNuevos() {
        Usuario u = servicio.crear("ana@empresa.mx", "Ana Ruiz", "clave-segura-1", Set.of(Rol.VENTAS));
        servicio.asignarRoles(u.getId(), Set.of(Rol.CONTADOR), "Cambio de área");
        EventoDominio evento = eventos.get(0);
        assertThat(evento.tipo()).isEqualTo(UsuarioService.EVENTO_ROL_ASIGNADO);
        assertThat(evento.datos()).containsEntry("rolesAnteriores", List.of("VENTAS"))
                .containsEntry("rolesNuevos", List.of("CONTADOR"));
    }

    @Test
    void credencialesDeUsuarioInexistenteNoRevelanLaExistencia() {
        assertThatThrownBy(() -> servicio.autenticar("nadie@empresa.mx", "x"))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }
}
