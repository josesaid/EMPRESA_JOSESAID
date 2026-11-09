package com.mx.josesaid.develop.ui.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class AgilizaIaOpcionesTest {

    @Test
    void cadaRolVeSusOpcionesConIdEstable() {
        List<SugerenciasIa.Opcion> opciones = SugerenciasIa.opciones(List.of("VENTAS"));

        assertThat(opciones).hasSize(2);
        assertThat(opciones).extracting(SugerenciasIa.Opcion::id).containsExactly("VENTAS-0", "VENTAS-1");
        assertThat(opciones).allSatisfy(o -> assertThat(o.rol()).isEqualTo("VENTAS"));
    }

    @Test
    void opcionDeOtroRolNoSeEligeNiSeDevuelve() {
        assertThat(SugerenciasIa.opcion(List.of("VENTAS"), "COMPRAS-0")).isEmpty();
    }

    @Test
    void opcionElegidaTraeSusPasos() {
        SugerenciasIa.Opcion elegida = SugerenciasIa.opcion(List.of("ALMACEN"), "ALMACEN-0").orElseThrow();

        assertThat(elegida.pasos()).isNotEmpty();
        assertThat(elegida.texto()).isNotBlank();
    }

    @Test
    void idInventadoOSinRolConocidoNoDaOpciones() {
        assertThat(SugerenciasIa.opcion(List.of("VENTAS"), "../admin")).isEmpty();
        assertThat(SugerenciasIa.opcion(List.of("VENTAS"), null)).isEmpty();
        assertThat(SugerenciasIa.opciones(List.of("DESCONOCIDO"))).isEmpty();
    }
}
