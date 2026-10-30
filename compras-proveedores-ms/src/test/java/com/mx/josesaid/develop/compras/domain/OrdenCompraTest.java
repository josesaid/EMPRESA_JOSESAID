package com.mx.josesaid.develop.compras.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class OrdenCompraTest {

    private static Proveedor activo() {
        return Proveedor.crear("Aceros del Norte SA", "AAN010101AB3", "ventas@aceros.mx");
    }

    private static List<LineaOrden> lineas() {
        return List.of(LineaOrden.nueva("TORN-1", new BigDecimal("10"), new BigDecimal("100")));
    }

    @Test
    void calculaTotalDeLasLineas() {
        OrdenCompra o = OrdenCompra.crear(activo(), "MXN", lineas());
        assertThat(o.total()).isEqualByComparingTo("1000.00");
        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.BORRADOR);
    }

    @Test
    void proveedorInactivoNoRecibeOrdenes() {
        Proveedor bloqueado = Proveedor.reconstruir(java.util.UUID.randomUUID(), "X", "AAN010101AB3",
                "a@b.mx", EstatusProveedor.BLOQUEADO);
        assertThatThrownBy(() -> OrdenCompra.crear(bloqueado, "MXN", lineas()))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void recepcionNoPuedeExcederLoOrdenado() {
        OrdenCompra o = OrdenCompra.crear(activo(), "MXN", lineas());
        o.aprobar();
        o.recibir("TORN-1", new BigDecimal("6"));
        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.RECIBIDA_PARCIAL);
        assertThatThrownBy(() -> o.recibir("TORN-1", new BigDecimal("5")))
                .isInstanceOf(ConflictoException.class);
        o.recibir("TORN-1", new BigDecimal("4"));
        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.RECIBIDA);
    }

    @Test
    void aprobadaSoloSeCancelaConMotivo() {
        OrdenCompra o = OrdenCompra.crear(activo(), "MXN", lineas());
        o.aprobar();
        assertThatThrownBy(() -> o.cancelar(" ")).isInstanceOf(SolicitudInvalidaException.class);
        assertThatThrownBy(() -> o.aprobar()).isInstanceOf(ConflictoException.class);
        o.cancelar("Proveedor sin stock");
        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.CANCELADA);
    }
}
