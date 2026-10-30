package com.mx.josesaid.develop.clientes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EstatusClienteTest {

    @Test
    void activoYSuspendidoPuedenAlternarse() {
        assertThat(EstatusCliente.ACTIVO.puedePasarA(EstatusCliente.SUSPENDIDO)).isTrue();
        assertThat(EstatusCliente.SUSPENDIDO.puedePasarA(EstatusCliente.ACTIVO)).isTrue();
    }

    @Test
    void bajaEsTerminal() {
        assertThat(EstatusCliente.BAJA.puedePasarA(EstatusCliente.ACTIVO)).isFalse();
        assertThat(EstatusCliente.ACTIVO.puedePasarA(EstatusCliente.BAJA)).isTrue();
    }
}
