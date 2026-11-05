package com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CampoCifradoConverterTest {

    @BeforeAll
    static void claves() {
        ClavesCripto.configurar("0123456789abcdef0123456789abcdef".getBytes(),
                "fedcba9876543210fedcba9876543210".getBytes());
    }

    @Test
    void idaYVueltaRecuperaElTexto() {
        CampoCifradoConverter c = new CampoCifradoConverter();
        String cifrado = c.convertToDatabaseColumn("contador@empresa.mx");
        assertThat(cifrado).isNotEqualTo("contador@empresa.mx");
        assertThat(c.convertToEntityAttribute(cifrado)).isEqualTo("contador@empresa.mx");
    }

    @Test
    void mismoTextoProduceCifradosDistintos() {
        CampoCifradoConverter c = new CampoCifradoConverter();
        assertThat(c.convertToDatabaseColumn("ana@empresa.mx"))
                .isNotEqualTo(c.convertToDatabaseColumn("ana@empresa.mx"));
    }

    @Test
    void indiceNoDependeDeMayusculasNiEspacios() {
        assertThat(IndiceCorreo.de("A@empresa.mx")).isEqualTo(IndiceCorreo.de(" a@EMPRESA.mx "));
    }
}
