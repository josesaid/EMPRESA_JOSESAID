package com.mx.josesaid.develop.inventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ExistenciaTest {

    private final Existencia ex = Existencia.vacia(UUID.randomUUID(), UUID.randomUUID());

    @Test
    void entradaYSalidaActualizanLaExistencia() {
        ex.aplicar(TipoMovimiento.ENTRADA, new BigDecimal("10"));
        ex.aplicar(TipoMovimiento.SALIDA, new BigDecimal("4"));
        assertThat(ex.getCantidad()).isEqualByComparingTo("6.000");
    }

    @Test
    void salidaMayorQueLaExistenciaSeRechazaConConflicto() {
        ex.aplicar(TipoMovimiento.ENTRADA, new BigDecimal("5"));
        assertThatThrownBy(() -> ex.aplicar(TipoMovimiento.SALIDA, new BigDecimal("6")))
                .isInstanceOf(ConflictoException.class)
                .hasMessageContaining("insuficiente");
    }

    @Test
    void reservaReduceLoDisponibleSinTocarLaExistencia() {
        ex.aplicar(TipoMovimiento.ENTRADA, new BigDecimal("10"));
        ex.aplicar(TipoMovimiento.RESERVA, new BigDecimal("7"));
        assertThat(ex.disponible()).isEqualByComparingTo("3.000");
        assertThatThrownBy(() -> ex.aplicar(TipoMovimiento.SALIDA, new BigDecimal("4")))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void ajusteNoPuedeDejarLaExistenciaNegativaNiBajoLoReservado() {
        ex.aplicar(TipoMovimiento.ENTRADA, new BigDecimal("10"));
        ex.aplicar(TipoMovimiento.RESERVA, new BigDecimal("8"));
        assertThatThrownBy(() -> ex.aplicar(TipoMovimiento.AJUSTE, new BigDecimal("-3")))
                .isInstanceOf(ConflictoException.class);
        ex.aplicar(TipoMovimiento.AJUSTE, new BigDecimal("-2"));
        assertThat(ex.getCantidad()).isEqualByComparingTo("8.000");
    }

    @Test
    void cantidadesInvalidasSeRechazan() {
        assertThatThrownBy(() -> ex.aplicar(TipoMovimiento.ENTRADA, BigDecimal.ZERO))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> ex.aplicar(TipoMovimiento.AJUSTE, BigDecimal.ZERO))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> ex.aplicar(TipoMovimiento.ENTRADA, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
