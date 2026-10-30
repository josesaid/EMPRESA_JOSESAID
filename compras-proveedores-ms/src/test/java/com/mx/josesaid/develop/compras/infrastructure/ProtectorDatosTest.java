package com.mx.josesaid.develop.compras.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.mx.josesaid.develop.compras.infrastructure.seguridad.ProtectorDatos;
import org.junit.jupiter.api.Test;

class ProtectorDatosTest {

    private static final String CLAVE = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private final ProtectorDatos protector = new ProtectorDatos(CLAVE, CLAVE);

    @Test
    void cifradoIdaYVueltaYNoRevelaElClaro() {
        String cifrado = protector.cifrar("AAN010101AB3");
        assertThat(cifrado).doesNotContain("AAN010101AB3");
        assertThat(protector.descifrar(cifrado)).isEqualTo("AAN010101AB3");
    }

    @Test
    void cadaCifradoEsDistintoPorqueUsaIvAleatorio() {
        assertThat(protector.cifrar("mismo")).isNotEqualTo(protector.cifrar("mismo"));
    }

    @Test
    void indiceEsDeterministaParaBuscarPorIgualdad() {
        assertThat(protector.indice("AAN010101AB3")).isEqualTo(protector.indice("AAN010101AB3"));
        assertThat(protector.indice("AAN010101AB3")).isNotEqualTo(protector.indice("AAN010101AB4"));
    }
}
