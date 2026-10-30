package com.mx.josesaid.develop.clientes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstatusProspectoTest {

    @Test
    void avanzaEnOrdenNuevoContactadoCalificado() {
        assertThat(EstatusProspecto.NUEVO.puedePasarA(EstatusProspecto.CONTACTADO)).isTrue();
        assertThat(EstatusProspecto.CONTACTADO.puedePasarA(EstatusProspecto.CALIFICADO)).isTrue();
        assertThat(EstatusProspecto.NUEVO.puedePasarA(EstatusProspecto.CALIFICADO)).isFalse();
    }

    @Test
    void convertidoSoloSeAlcanzaPorConversion() {
        assertThat(EstatusProspecto.CALIFICADO.puedePasarA(EstatusProspecto.CONVERTIDO)).isFalse();
    }

    @Test
    void perdidoSoloDesdeEstadosAbiertos() {
        assertThat(EstatusProspecto.CONTACTADO.puedePasarA(EstatusProspecto.PERDIDO)).isTrue();
        assertThat(EstatusProspecto.PERDIDO.puedePasarA(EstatusProspecto.NUEVO)).isFalse();
    }
}
