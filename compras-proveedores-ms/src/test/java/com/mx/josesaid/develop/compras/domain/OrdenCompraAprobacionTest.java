package com.mx.josesaid.develop.compras.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OrdenCompraAprobacionTest {

    private static final BigDecimal TIPO_CAMBIO = new BigDecimal("18");

    private static Proveedor activo() {
        return Proveedor.crear("Aceros del Norte SA", "AAN010101AB3", "ventas@aceros.mx");
    }

    /** Orden de un solo renglón con el total indicado. */
    private static OrdenCompra orden(String moneda, String total, String creadaPor) {
        List<LineaOrden> lineas = List.of(LineaOrden.nueva("TORN-1", BigDecimal.ONE, new BigDecimal(total)));
        return OrdenCompra.crear(activo(), moneda, lineas, creadaPor);
    }

    @Test
    void comprasApruebaUnaOrdenMenorAlLimiteQueNoCreo() {
        OrdenCompra o = orden("MXN", "1000", "u-compras-1");

        o.aprobar("u-compras-2", false, TIPO_CAMBIO);

        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.APROBADA);
    }

    @Test
    void nadieApruebaUnaOrdenQueCreo() {
        OrdenCompra o = orden("MXN", "1000", "u-compras-1");

        assertThatThrownBy(() -> o.aprobar("u-compras-1", false, TIPO_CAMBIO))
                .isInstanceOf(AprobacionNoPermitidaException.class)
                .hasMessageContaining("tú creaste");
        assertThatThrownBy(() -> o.aprobar("u-compras-1", true, TIPO_CAMBIO))
                .isInstanceOf(AprobacionNoPermitidaException.class);
        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.BORRADOR);
    }

    @Test
    void direccionApruebaUnaOrdenQueOtroCreo() {
        OrdenCompra o = orden("MXN", "1000", "u-compras-1");

        o.aprobar("u-direccion", true, TIPO_CAMBIO);

        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.APROBADA);
    }

    @Test
    void comprasNoApruebaUnaOrdenDeUnMillonDeDolaresOMas() {
        OrdenCompra o = orden("USD", "1000000", "u-compras-1");

        assertThatThrownBy(() -> o.aprobar("u-compras-2", false, TIPO_CAMBIO))
                .isInstanceOf(AprobacionNoPermitidaException.class)
                .hasMessageContaining("1,000,000 USD");
    }

    @Test
    void unaOrdenEnPesosSeConvierteAUsdParaComparar() {
        // 18,000,000 MXN a 18 MXN por USD son exactamente 1,000,000 USD: el límite es inclusivo.
        OrdenCompra enLimite = orden("MXN", "18000000", "u-compras-1");
        assertThatThrownBy(() -> enLimite.aprobar("u-compras-2", false, TIPO_CAMBIO))
                .isInstanceOf(AprobacionNoPermitidaException.class);

        // 17,999,999 MXN son menos de 1,000,000 USD: COMPRAS sí aprueba.
        OrdenCompra debajo = orden("MXN", "17999999", "u-compras-1");
        debajo.aprobar("u-compras-2", false, TIPO_CAMBIO);
        assertThat(debajo.getEstatus()).isEqualTo(EstatusOrdenCompra.APROBADA);
    }

    @Test
    void direccionApruebaUnaOrdenDeUnMillonDeDolaresOMas() {
        OrdenCompra o = orden("USD", "2000000", "u-compras-1");

        o.aprobar("u-direccion", true, TIPO_CAMBIO);

        assertThat(o.getEstatus()).isEqualTo(EstatusOrdenCompra.APROBADA);
    }

    @Test
    void unaOrdenAntiguaSinCreadorSoloLaApruebaDireccionAunqueSeaChica() {
        OrdenCompra antigua = OrdenCompra.reconstruir(UUID.randomUUID(), UUID.randomUUID(), "MXN",
                EstatusOrdenCompra.BORRADOR, null,
                List.of(LineaOrden.nueva("TORN-1", BigDecimal.ONE, new BigDecimal("100"))), null);

        assertThatThrownBy(() -> antigua.aprobar("u-compras-2", false, TIPO_CAMBIO))
                .isInstanceOf(AprobacionNoPermitidaException.class)
                .hasMessageContaining("antes de registrar su creador");

        antigua.aprobar("u-direccion", true, TIPO_CAMBIO);
        assertThat(antigua.getEstatus()).isEqualTo(EstatusOrdenCompra.APROBADA);
    }

    @Test
    void laOrdenNecesitaUnCreador() {
        assertThatThrownBy(() -> orden("MXN", "1000", " "))
                .isInstanceOf(ReglaDeNegocioException.class);
    }
}
