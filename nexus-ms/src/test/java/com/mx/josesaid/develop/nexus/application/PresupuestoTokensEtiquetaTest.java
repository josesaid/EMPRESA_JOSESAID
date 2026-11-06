package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PresupuestoTokensEtiquetaTest {

    @Test
    void noRegistraElCorreoEnClaro() {
        String etiqueta = PresupuestoTokens.etiqueta("ana@empresa.com");

        assertThat(etiqueta).startsWith("u-").doesNotContain("ana").doesNotContain("@");
    }

    @Test
    void mismoUsuarioMismaEtiquetaSinImportarMayusculas() {
        assertThat(PresupuestoTokens.etiqueta("Ana@Empresa.com"))
                .isEqualTo(PresupuestoTokens.etiqueta("ana@empresa.com"));
    }

    @Test
    void usuariosDistintosDistintaEtiqueta() {
        assertThat(PresupuestoTokens.etiqueta("ana@empresa.com"))
                .isNotEqualTo(PresupuestoTokens.etiqueta("luis@empresa.com"));
    }

    @Test
    void sinUsuarioDevuelveDesconocido() {
        assertThat(PresupuestoTokens.etiqueta(null)).isEqualTo("desconocido");
        assertThat(PresupuestoTokens.etiqueta("  ")).isEqualTo("desconocido");
    }
}
