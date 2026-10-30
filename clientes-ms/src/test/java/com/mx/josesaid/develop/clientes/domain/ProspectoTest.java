package com.mx.josesaid.develop.clientes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mx.josesaid.develop.clientes.infrastructure.crypto.ClavesCripto;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProspectoTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T18:00:00Z");

    @BeforeAll
    static void claves() {
        ClavesCripto.configurar(new byte[32], new byte[32]);
    }

    private Prospecto nuevo() {
        return Prospecto.nuevo(UUID.randomUUID(), "Ana López", "ana@ejemplo.mx", null,
                Fuente.CAMPANA, LocalDate.parse("2026-10-29"), AHORA);
    }

    @Test
    void nuevoProspectoInicia_enNuevoConIndicesCalculados() {
        Prospecto prospecto = nuevo();

        assertThat(prospecto.getEstatus()).isEqualTo(EstatusProspecto.NUEVO);
        assertThat(prospecto.getCorreoHash()).hasSize(64);
        assertThat(prospecto.getNombreHash()).hasSize(64);
    }

    @Test
    void noSePuedeConvertirDesdeNuevo() {
        Prospecto prospecto = nuevo();

        assertThatThrownBy(() -> prospecto.marcarConvertido(AHORA))
                .isInstanceOf(TransicionInvalidaException.class);
    }

    @Test
    void conviertePorqueEstaCalificado() {
        Prospecto prospecto = nuevo();
        prospecto.cambiarEstatus(EstatusProspecto.CONTACTADO, AHORA);
        prospecto.cambiarEstatus(EstatusProspecto.CALIFICADO, AHORA);

        prospecto.marcarConvertido(AHORA);

        assertThat(prospecto.getEstatus()).isEqualTo(EstatusProspecto.CONVERTIDO);
        assertThat(prospecto.getFechaConversion()).isEqualTo(AHORA);
    }
}
