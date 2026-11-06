package com.mx.josesaid.develop.nexus.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class LimpiezaAccionesTest {

    @Test
    void borraLasVencidasAntesDeUnaHoraAtras() {
        Instant ahora = Instant.parse("2026-11-05T12:00:00Z");
        Clock reloj = Clock.fixed(ahora, ZoneOffset.UTC);
        AccionPendienteRepositorio repo = mock(AccionPendienteRepositorio.class);
        when(repo.borrarVencidasAntesDe(any())).thenReturn(3);

        int borradas = new LimpiezaAcciones(repo, reloj).borrarVencidas();

        verify(repo).borrarVencidasAntesDe(ahora.minus(Duration.ofHours(1)));
        assertThat(borradas).isEqualTo(3);
    }
}
