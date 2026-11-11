package com.mx.josesaid.develop.inventario.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RevocacionSesionesTest {

    private final RevocacionSesiones revocaciones = new RevocacionSesiones();

    @Test
    void revocaSesionVigenteYLaReconoce() {
        revocaciones.revocarSesion("sid-1", Instant.now());

        assertThat(revocaciones.revocado("sid-1", null, null)).isTrue();
        assertThat(revocaciones.revocado("sid-otra", null, null)).isFalse();
    }

    @Test
    void ignoraSesionSinIdentificadorOSinFecha() {
        revocaciones.revocarSesion(null, Instant.now());
        revocaciones.revocarSesion("sid-2", null);

        assertThat(revocaciones.revocado(null, null, null)).isFalse();
        assertThat(revocaciones.revocado("sid-2", null, null)).isFalse();
    }

    @Test
    void ignoraRevocacionYaVencida() {
        Instant hace = Instant.now().minus(RevocacionSesiones.VIDA).minusSeconds(60);

        revocaciones.revocarSesion("sid-vieja", hace);
        revocaciones.revocarUsuario("usuario-viejo", hace);

        assertThat(revocaciones.revocado("sid-vieja", null, null)).isFalse();
        assertThat(revocaciones.revocado(null, "usuario-viejo", Instant.now().minusSeconds(3600))).isFalse();
    }

    @Test
    void revocaTokensDelUsuarioEmitidosAntesDelCierre() {
        Instant cierre = Instant.now();
        revocaciones.revocarUsuario("usuario-1", cierre);

        assertThat(revocaciones.revocado(null, "usuario-1", cierre.minusSeconds(5))).isTrue();
        assertThat(revocaciones.revocado(null, "usuario-1", cierre.plusSeconds(5))).isFalse();
    }

    @Test
    void conservaLaFechaDeCierreMasReciente() {
        Instant reciente = Instant.now();
        revocaciones.revocarUsuario("usuario-2", reciente);
        revocaciones.revocarUsuario("usuario-2", reciente.minusSeconds(60));

        assertThat(revocaciones.revocado(null, "usuario-2", reciente.minusSeconds(5))).isTrue();
    }

    @Test
    void ignoraUsuarioNuloYEmitidoNulo() {
        revocaciones.revocarUsuario(null, Instant.now());
        revocaciones.revocarUsuario("usuario-3", Instant.now());

        assertThat(revocaciones.revocado(null, null, Instant.now().minusSeconds(60))).isFalse();
        assertThat(revocaciones.revocado(null, "usuario-3", null)).isFalse();
    }
}
