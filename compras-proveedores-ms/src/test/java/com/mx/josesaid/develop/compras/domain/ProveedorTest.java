package com.mx.josesaid.develop.compras.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ProveedorTest {

    @Test
    void normalizaRfcYCorreo() {
        Proveedor p = Proveedor.crear("Aceros del Norte SA", " aan010101ab3 ", "Ventas@Aceros.MX");
        assertThat(p.getRfc()).isEqualTo("AAN010101AB3");
        assertThat(p.getCorreo()).isEqualTo("ventas@aceros.mx");
        assertThat(p.estaActivo()).isTrue();
    }

    @Test
    void rfcInvalidoSeRechaza() {
        assertThatThrownBy(() -> Proveedor.crear("X", "ABC", "a@b.mx"))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("RFC");
    }

    @Test
    void correoInvalidoSeRechaza() {
        assertThatThrownBy(() -> Proveedor.crear("X", "AAN010101AB3", "sin-arroba"))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
