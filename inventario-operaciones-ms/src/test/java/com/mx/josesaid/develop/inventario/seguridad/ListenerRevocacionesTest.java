package com.mx.josesaid.develop.inventario.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class ListenerRevocacionesTest {

    private final RevocacionSesiones revocaciones = new RevocacionSesiones();
    private ListenerRevocaciones listener;

    @BeforeEach
    void preparar() {
        listener = new ListenerRevocaciones(revocaciones, new ObjectMapper());
    }

    @Test
    void aplicaCierreDeSesion() {
        Instant ahora = Instant.now();
        listener.recibir("""
                {"tipo":"sesion-cerrada","agregadoId":"u1","datos":{"sid":"sid-9","ocurridoEn":"%s"}}
                """.formatted(ahora));

        assertThat(revocaciones.revocado("sid-9", null, null)).isTrue();
    }

    @Test
    void aplicaCierreDeTodasLasSesionesDelUsuario() {
        Instant ahora = Instant.now();
        listener.recibir("""
                {"tipo":"sesiones-usuario-cerradas","agregadoId":"u2","datos":{"usuarioId":"u2","ocurridoEn":"%s"}}
                """.formatted(ahora));

        assertThat(revocaciones.revocado(null, "u2", ahora.minusSeconds(10))).isTrue();
    }

    @Test
    void ignoraEventosAjenos() {
        listener.recibir("""
                {"tipo":"usuario-creado","agregadoId":"u3","datos":{"sid":"sid-x","ocurridoEn":"%s"}}
                """.formatted(Instant.now()));

        assertThat(revocaciones.revocado("sid-x", null, null)).isFalse();
    }

    @Test
    void descartaMensajeMalformadoSinPropagarlo() {
        listener.recibir("no es json");
        listener.recibir("{\"tipo\":\"sesion-cerrada\",\"datos\":{\"sid\":\"s\",\"ocurridoEn\":\"no-fecha\"}}");
        listener.recibir("{\"tipo\":\"sesion-cerrada\",\"agregadoId\":\"u\"}");

        assertThat(revocaciones.revocado("s", null, null)).isFalse();
    }
}
