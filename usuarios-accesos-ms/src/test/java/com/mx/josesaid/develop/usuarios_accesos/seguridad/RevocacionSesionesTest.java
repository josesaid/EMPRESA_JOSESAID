package com.mx.josesaid.develop.usuarios_accesos.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class RevocacionSesionesTest {

    private static final Instant AHORA = Instant.now();

    @Test
    void unaSesionCerradaRechazaSuSidYNoLasDemas() {
        RevocacionSesiones revocaciones = new RevocacionSesiones();
        revocaciones.revocarSesion("sesion-1", AHORA);
        assertThat(revocaciones.revocado("sesion-1", "u1", AHORA.minusSeconds(60))).isTrue();
        assertThat(revocaciones.revocado("sesion-2", "u1", AHORA.minusSeconds(60))).isFalse();
    }

    @Test
    void cerrarTodasRechazaSoloLosTokensAnterioresAlCierre() {
        RevocacionSesiones revocaciones = new RevocacionSesiones();
        revocaciones.revocarUsuario("u1", AHORA);
        assertThat(revocaciones.revocado(null, "u1", AHORA.minusSeconds(60))).isTrue();
        assertThat(revocaciones.revocado(null, "u1", AHORA.plusSeconds(60))).isFalse();
        assertThat(revocaciones.revocado(null, "u2", AHORA.minusSeconds(60))).isFalse();
    }

    @Test
    void unaRevocacionVencidaSeIgnora() {
        RevocacionSesiones revocaciones = new RevocacionSesiones();
        revocaciones.revocarSesion("sesion-1", AHORA.minus(Duration.ofHours(1)));
        assertThat(revocaciones.revocado("sesion-1", "u1", AHORA.minus(Duration.ofHours(2)))).isFalse();
    }
}
