package com.mx.josesaid.develop.nexus.comun.filtro;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;

import org.junit.jupiter.api.Test;

class FiltroRfcOmitidoTest {

    @Test
    void conservaElRfcCuandoSeOmite() {
        var resultado = FiltroDatosSensibles.redactar("Cliente PRU000101XAB, correo a@b.com",
                EnumSet.of(FiltroDatosSensibles.Tipo.RFC));

        assertEquals("Cliente PRU000101XAB, correo [CORREO]", resultado.texto());
    }

    @Test
    void enmascaraElRfcPorDefecto() {
        assertEquals("Cliente [RFC]", FiltroDatosSensibles.redactar("Cliente PRU000101XAB").texto());
    }
}
