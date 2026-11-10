package com.mx.josesaid.develop.nexus.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TemasEmpresaTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "¿Cuántas existencias hay del SKU-001?",
            "Dame el saldo del proveedor Acme",
            "¿Qué comprobantes siguen pendientes de cobro?",
            "Resumen de facturación del mes",
            "¿Qué contratos vencen?",
            "Revisa el RFC XAXX010101000",
            "hola",
            "¡Buenos días!"
    })
    void dejaPasarPreguntasDeEmpresa(String pregunta) {
        assertThat(TemasEmpresa.esDeEmpresa(pregunta)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "¿Cuánto es 2*2?",
            "¿Quién ganó el mundial?",
            "Escribe un poema sobre el mar",
            "Hola, ¿cuánto es 2*2?",
            "Dame una receta de pasta",
            "",
            "   "
    })
    void rechazaPreguntasAjenas(String pregunta) {
        assertThat(TemasEmpresa.esDeEmpresa(pregunta)).isFalse();
    }
}
