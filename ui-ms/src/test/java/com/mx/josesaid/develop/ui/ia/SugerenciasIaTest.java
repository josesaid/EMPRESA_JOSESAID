package com.mx.josesaid.develop.ui.ia;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SugerenciasIaTest {

    private static final LocalTime AHORA = LocalTime.of(9, 5);

    @Test
    void rolVentasRecibeSusSugerenciasConLaHora() {
        List<SugerenciasIa.Sugerencia> log = SugerenciasIa.para(List.of("VENTAS"), AHORA);

        assertThat(log).hasSize(2);
        assertThat(log).allSatisfy(s -> {
            assertThat(s.rol()).isEqualTo("VENTAS");
            assertThat(s.hora()).isEqualTo("09:05");
        });
    }

    @Test
    void conVariosRolesVanPrimeroLosDeMayorPrioridad() {
        List<SugerenciasIa.Sugerencia> log = SugerenciasIa.para(List.of("COMPRAS", "ADMIN"), AHORA);

        assertThat(log).extracting(SugerenciasIa.Sugerencia::rol)
                .containsExactly("ADMIN", "ADMIN", "COMPRAS", "COMPRAS");
    }

    @Test
    void sinRolesConocidosNoHayLog() {
        assertThat(SugerenciasIa.para(List.of("DESCONOCIDO"), AHORA)).isEmpty();
        assertThat(SugerenciasIa.para(List.of(), AHORA)).isEmpty();
    }
}
