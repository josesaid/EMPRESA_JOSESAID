package com.mx.josesaid.develop.clientes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsentimientoTest {

    private static final Instant AHORA = Instant.parse("2026-10-29T18:00:00Z");

    private Consentimiento otorgado(LocalDate vigencia) {
        return Consentimiento.otorgado(UUID.randomUUID(), UUID.randomUUID(), "Comunicaciones comerciales",
                AHORA, MedioConsentimiento.FORMULARIO, vigencia, AHORA);
    }

    @Test
    void revocarEsIdempotenteYSoloEmiteLaPrimeraVez() {
        Consentimiento consentimiento = otorgado(null);

        assertThat(consentimiento.revocar(AHORA)).isTrue();
        assertThat(consentimiento.revocar(AHORA.plusSeconds(60))).isFalse();
        assertThat(consentimiento.getRevocadoAt()).isEqualTo(AHORA);
    }

    @Test
    void noEstaVigenteDespuesDeSuVigencia() {
        Consentimiento consentimiento = otorgado(LocalDate.parse("2027-10-29"));

        assertThat(consentimiento.estaVigente(LocalDate.parse("2027-10-29"))).isTrue();
        assertThat(consentimiento.estaVigente(LocalDate.parse("2027-10-30"))).isFalse();
    }
}
