package com.mx.josesaid.develop.clientes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InteraccionTest {

    @Test
    void nuevaGuardaLosDatosRecibidos() {
        UUID id = UUID.randomUUID();
        UUID clienteId = UUID.randomUUID();
        UUID responsableId = UUID.randomUUID();
        Instant fecha = Instant.parse("2026-11-01T15:30:00Z");
        Instant ahora = Instant.parse("2026-11-02T10:00:00Z");

        Interaccion i = Interaccion.nueva(id, clienteId, TipoInteraccion.VISITA, fecha, responsableId,
                "Revisión de contrato", ahora);

        assertThat(i.getId()).isEqualTo(id);
        assertThat(i.getClienteId()).isEqualTo(clienteId);
        assertThat(i.getTipo()).isEqualTo(TipoInteraccion.VISITA);
        assertThat(i.getFecha()).isEqualTo(fecha);
        assertThat(i.getResponsableId()).isEqualTo(responsableId);
        assertThat(i.getResumen()).isEqualTo("Revisión de contrato");
    }

    @Test
    void nuevaQuitaEspaciosAlrededorDelResumen() {
        Interaccion i = Interaccion.nueva(UUID.randomUUID(), UUID.randomUUID(), TipoInteraccion.LLAMADA,
                Instant.now(), UUID.randomUUID(), "   seguimiento de cobro  \n", Instant.now());

        assertThat(i.getResumen()).isEqualTo("seguimiento de cobro");
    }
}
