package com.mx.josesaid.develop.inventario.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class OrdenOperacionTest {

    @Test
    void cerrarRequiereCantidadRealMayorACero() {
        OrdenOperacion orden = OrdenOperacion.crear(TipoOrden.PRODUCCION, new BigDecimal("50"));
        assertThatThrownBy(() -> orden.cerrar(BigDecimal.ZERO)).isInstanceOf(ReglaDeNegocioException.class);
        orden.cerrar(new BigDecimal("48"));
        assertThat(orden.getEstatus()).isEqualTo(EstatusOrden.CERRADA);
        assertThat(orden.getCantidadReal()).isEqualByComparingTo("48.000");
    }

    @Test
    void ordenCerradaNoSeCierraDeNuevo() {
        OrdenOperacion orden = OrdenOperacion.crear(TipoOrden.SERVICIO, BigDecimal.ONE);
        orden.cerrar(BigDecimal.ONE);
        assertThatThrownBy(() -> orden.cerrar(BigDecimal.ONE)).isInstanceOf(ConflictoException.class);
    }
}
