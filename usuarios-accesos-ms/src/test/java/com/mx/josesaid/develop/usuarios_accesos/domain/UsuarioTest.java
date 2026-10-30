package com.mx.josesaid.develop.usuarios_accesos.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T15:00:00Z");

    private Usuario nuevo() {
        return Usuario.crear("  Ana@Empresa.MX ", "Ana Ruiz", "hash", List.of(Rol.VENTAS));
    }

    @Test
    void normalizaElCorreoAMinusculasSinEspacios() {
        assertThat(nuevo().getCorreo()).isEqualTo("ana@empresa.mx");
    }

    @Test
    void rechazaUsuarioSinRoles() {
        assertThatThrownBy(() -> Usuario.crear("a@b.mx", "Ana", "hash", List.of()))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("al menos un rol");
    }

    @Test
    void rechazaCorreoSinArroba() {
        assertThatThrownBy(() -> Usuario.normalizarCorreo("ana.empresa.mx"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void bloqueaEnElQuintoIntentoFallido() {
        Usuario u = nuevo();
        for (int i = 1; i < Usuario.MAX_INTENTOS_FALLIDOS; i++) {
            assertThat(u.registrarIntentoFallido(AHORA)).isFalse();
        }
        assertThat(u.registrarIntentoFallido(AHORA)).isTrue();
        assertThat(u.getEstatus()).isEqualTo(EstatusUsuario.BLOQUEADO);
    }

    @Test
    void cuentaBloqueadaNoPuedeIntentarAccesoAntesDelPlazo() {
        Usuario u = nuevo();
        for (int i = 0; i < Usuario.MAX_INTENTOS_FALLIDOS; i++) {
            u.registrarIntentoFallido(AHORA);
        }
        assertThatThrownBy(() -> u.verificarAcceso(AHORA.plusSeconds(60)))
                .isInstanceOf(BloqueoException.class);
    }

    @Test
    void desbloqueaCuandoVenceElPlazo() {
        Usuario u = nuevo();
        for (int i = 0; i < Usuario.MAX_INTENTOS_FALLIDOS; i++) {
            u.registrarIntentoFallido(AHORA);
        }
        u.verificarAcceso(AHORA.plus(Usuario.DURACION_BLOQUEO).plusSeconds(1));
        assertThat(u.getEstatus()).isEqualTo(EstatusUsuario.ACTIVO);
        assertThat(u.getIntentosFallidos()).isZero();
    }

    @Test
    void loginExitosoReiniciaLosIntentos() {
        Usuario u = nuevo();
        u.registrarIntentoFallido(AHORA);
        u.registrarIntentoFallido(AHORA);
        u.registrarLoginExitoso();
        assertThat(u.getIntentosFallidos()).isZero();
    }

    @Test
    void bajaEsDefinitivaYNoPermiteAcceso() {
        Usuario u = nuevo();
        u.darDeBaja();
        assertThatThrownBy(() -> u.verificarAcceso(AHORA))
                .isInstanceOf(CredencialesInvalidasException.class);
        assertThatThrownBy(u::darDeBaja).isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void asignarRolesRequiereMotivo() {
        Usuario u = nuevo();
        assertThatThrownBy(() -> u.asignarRoles(List.of(Rol.ADMIN), " "))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("motivo");
    }

    @Test
    void asignarRolesReemplazaLosRoles() {
        Usuario u = nuevo();
        u.asignarRoles(List.of(Rol.CONTADOR, Rol.FINANZAS), "Cambio de área");
        assertThat(u.getRoles()).containsExactlyInAnyOrder(Rol.CONTADOR, Rol.FINANZAS);
    }

    @Test
    void noPermiteAsignarRolesAUsuarioDadoDeBaja() {
        Usuario u = nuevo();
        u.darDeBaja();
        assertThatThrownBy(() -> u.asignarRoles(List.of(Rol.ADMIN), "motivo"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
