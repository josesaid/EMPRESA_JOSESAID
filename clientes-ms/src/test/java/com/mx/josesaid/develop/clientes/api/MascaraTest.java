package com.mx.josesaid.develop.clientes.api;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MascaraTest {

    @Test
    void enmascaraRfcConservandoTresPrimerosYDosUltimos() {
        assertThat(Mascara.rfc("IDE900101XY1")).isEqualTo("IDE" + "*".repeat(7) + "Y1");
    }

    @Test
    void enmascaraCorreoConservandoPrimeraLetraYDominio() {
        assertThat(Mascara.correo("ana.lopez@ejemplo.mx")).isEqualTo("a***@ejemplo.mx");
    }

    @Test
    void enmascaraNombreDejandoSoloLaPrimeraLetra() {
        assertThat(Mascara.nombre("Ana López")).isEqualTo("A***");
    }
}
