package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class DatosAltaTest {

    @Test
    void leeCorreoNombreYRolesDelPayload() {
        DatosAlta datos = DatosAlta.de("{\"correo\":\"ana@empresa.com\",\"nombreCompleto\":\"Ana López\","
                + "\"roles\":[\"VENTAS\",\"CONTADOR\"]}");

        assertThat(datos.correo()).isEqualTo("ana@empresa.com");
        assertThat(datos.nombreCompleto()).isEqualTo("Ana López");
        assertThat(datos.roles()).containsExactly("VENTAS", "CONTADOR");
    }

    @Test
    void payloadInvalidoFalla() {
        assertThatThrownBy(() -> DatosAlta.de("no es json"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void listaVaciaSeConservaComoListaVacia() {
        assertThat(DatosAlta.de("{\"correo\":\"a@b.co\",\"nombreCompleto\":\"A\",\"roles\":[]}").roles())
                .isEqualTo(List.of());
    }
}
