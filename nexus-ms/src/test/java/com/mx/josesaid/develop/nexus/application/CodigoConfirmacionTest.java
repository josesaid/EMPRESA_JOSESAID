package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CodigoConfirmacionTest {

    private static final UUID ACCION = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private final CodigoConfirmacion codigos = new CodigoConfirmacion(
            "secreto-de-prueba-de-32-bytes-ok!".getBytes(StandardCharsets.UTF_8));

    @Test
    void elCodigoTieneOchoDigitosYEsEstable() {
        String codigo = codigos.generar(ACCION, "contador1");

        assertThat(codigo).matches("\\d{8}");
        assertThat(codigos.generar(ACCION, "contador1")).isEqualTo(codigo);
    }

    @Test
    void elCodigoDependeDelUsuarioQueLaPrepara() {
        assertThat(codigos.generar(ACCION, "contador1")).isNotEqualTo(codigos.generar(ACCION, "otro"));
    }

    @Test
    void coincideSoloConElCodigoCorrecto() {
        String codigo = codigos.generar(ACCION, "contador1");

        assertThat(codigos.coincide(ACCION, "contador1", codigo)).isTrue();
        assertThat(codigos.coincide(ACCION, "contador1", " " + codigo + " ")).isTrue();
        assertThat(codigos.coincide(ACCION, "contador1", "00000000")).isFalse();
        assertThat(codigos.coincide(ACCION, "contador1", null)).isFalse();
    }

    @Test
    void unSecretoCortoSeRechaza() {
        assertThatThrownBy(() -> new CodigoConfirmacion(new byte[4]))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
