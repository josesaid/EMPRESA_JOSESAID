package com.mx.josesaid.develop.nexus.comun.filtro;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles.Tipo;

class FiltroDatosSensiblesTest {

    @Test
    void redactaRfcDePersonaFisicaYMoral() {
        var r = FiltroDatosSensibles.redactar("Cliente XAXX010101000 y empresa MOGJ850315AB3.");
        assertThat(r.texto()).isEqualTo("Cliente [RFC] y empresa [RFC].");
        assertThat(r.conteo()).containsEntry(Tipo.RFC, 2);
    }

    @Test
    void redactaCorreo() {
        var r = FiltroDatosSensibles.redactar("Escribe a juan.perez+ventas@empresa.com.mx hoy.");
        assertThat(r.texto()).isEqualTo("Escribe a [CORREO] hoy.");
        assertThat(r.conteo()).containsEntry(Tipo.CORREO, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "Llama al 33 1234 5678.",
        "Llama al 3312345678.",
        "Llama al +52 55-1234-5678.",
        "Llama al 55 1234-5678."
    })
    void redactaTelefonoMexicano(String texto) {
        var r = FiltroDatosSensibles.redactar(texto);
        assertThat(r.texto()).contains("[TELEFONO]");
        assertThat(r.conteo()).containsKey(Tipo.TELEFONO);
    }

    @Test
    void redactaClabeDe18Digitos() {
        var r = FiltroDatosSensibles.redactar("CLABE 012180001234567891.");
        assertThat(r.texto()).isEqualTo("CLABE [CUENTA].");
        assertThat(r.conteo()).containsEntry(Tipo.CLABE, 1);
    }

    @Test
    void redactaTarjetaDe16Digitos() {
        var r = FiltroDatosSensibles.redactar("Tarjeta 4111 1111 1111 1111 vence 12/28.");
        assertThat(r.texto()).isEqualTo("Tarjeta [TARJETA] vence 12/28.");
        assertThat(r.conteo()).containsEntry(Tipo.TARJETA, 1);
    }

    // Falsos positivos: montos, folios, años e IDs no deben redactarse.
    @ParameterizedTest
    @ValueSource(strings = {
        "Factura por 1,234,567.89 pesos.",
        "Total 1,234.56 MXN.",
        "Folio 1234567890123 del 2026.",
        "Pedido 1234 cliente 55 con monto 9999.50.",
        "Versión 2.0.2 del BOM de Spring AI.",
        "Vence el 12/28 a las 18:30."
    })
    void noRedactaMontosFoliosYAnios(String texto) {
        var r = FiltroDatosSensibles.redactar(texto);
        assertThat(r.texto()).isEqualTo(texto);
        assertThat(r.total()).isZero();
    }

    @ParameterizedTest
    @CsvSource({
        "'Dato sin nada sensible',",
        "'',"
    })
    void textoSinDatosQuedaIgual(String texto, String esperado) {
        var r = FiltroDatosSensibles.redactar(texto);
        assertThat(r.texto()).isEqualTo(texto);
        assertThat(r.total()).isZero();
    }

    @Test
    void nuloNoFalla() {
        var r = FiltroDatosSensibles.redactar(null);
        assertThat(r.texto()).isNull();
        assertThat(r.total()).isZero();
    }

    @Test
    void cuentaVariosTiposEnUnMismoTexto() {
        var r = FiltroDatosSensibles.redactar(
                "RFC XAXX010101000, correo a@b.com, tel 3312345678, CLABE 012180001234567891.");
        assertThat(r.total()).isEqualTo(4);
        assertThat(r.texto()).doesNotContain("XAXX010101000", "a@b.com", "3312345678", "012180001234567891");
    }
}
