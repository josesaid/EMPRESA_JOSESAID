package com.mx.josesaid.develop.pedidos.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PedidoCotizacionTest {

    private static final UUID CLIENTE = UUID.randomUUID();
    private static final UUID PRODUCTO = UUID.randomUUID();

    private static Linea linea(int cantidad, String precio) {
        return Linea.crear(PRODUCTO, "Tornillo", cantidad, new BigDecimal(precio));
    }

    @Test
    void calculaSubtotalIvaYTotalRedondeadosALaMoneda() {
        Totales t = Totales.calcular(List.of(linea(2, "10.00")), "MXN", TasaIva.IVA_GENERAL);
        assertEquals(new BigDecimal("20.00"), t.subtotal());
        assertEquals(new BigDecimal("3.20"), t.iva());
        assertEquals(new BigDecimal("23.20"), t.total());
    }

    @Test
    void monedaFueraDeCatalogoSeRechaza() {
        assertThrows(ReglaDeNegocioException.class,
                () -> Totales.calcular(List.of(linea(1, "1")), "EUR", TasaIva.IVA_GENERAL));
    }

    @Test
    void cotizacionVencidaQuedaMarcadaYNoSeAcepta() {
        Cotizacion c = Cotizacion.crear(CLIENTE, Canal.WEB, "MXN", TasaIva.IVA_GENERAL,
                LocalDate.of(2026, 1, 1), List.of(linea(1, "5")));
        c.enviar();
        assertThrows(CotizacionVencidaException.class, () -> c.aceptar(LocalDate.of(2026, 2, 1)));
        assertEquals(EstatusCotizacion.VENCIDA, c.getEstatus());
    }

    @Test
    void noSeConvierteEnPedidoUnaCotizacionNoAceptada() {
        Cotizacion c = Cotizacion.crear(CLIENTE, Canal.WEB, "MXN", TasaIva.IVA_GENERAL,
                LocalDate.of(2030, 1, 1), List.of(linea(1, "5")));
        c.enviar();
        assertThrows(ConflictoException.class, () -> Pedido.desdeCotizacion(c));
    }

    @Test
    void noSeConfirmaUnPedidoSinLineas() {
        Pedido p = Pedido.crear(CLIENTE, Canal.TIENDA, "USD", TasaIva.IVA_EXENTO, List.of(linea(1, "3")));
        p.reemplazarLineas(List.of());
        assertThrows(ReglaDeNegocioException.class, () -> p.confirmar(Instant.now()));
    }

    @Test
    void devolucionSoloDesdeEntregadoYQuedaCancelada() {
        Pedido p = Pedido.crear(CLIENTE, Canal.WEB, "MXN", TasaIva.IVA_GENERAL, List.of(linea(1, "100")));
        assertThrows(ConflictoException.class, p::devolver);
        p.confirmar(Instant.now());
        p.entregar(Instant.now());
        p.devolver();
        assertEquals(EstatusPedido.CANCELADO, p.getEstatus());
        assertEquals(MotivoCancelacion.DEVOLUCION_TOTAL, p.getMotivoCancelacion());
    }

    @Test
    void laDevolucionNoSePuedeCancelarManualmente() {
        Pedido p = Pedido.crear(CLIENTE, Canal.WEB, "MXN", TasaIva.IVA_GENERAL, List.of(linea(1, "100")));
        assertThrows(SolicitudInvalidaException.class, () -> p.cancelar(MotivoCancelacion.DEVOLUCION_TOTAL));
    }

    @Test
    void unPedidoEntregadoNoSePuedeCancelar() {
        Pedido p = Pedido.crear(CLIENTE, Canal.WEB, "MXN", TasaIva.IVA_GENERAL, List.of(linea(1, "100")));
        p.confirmar(Instant.now());
        p.entregar(Instant.now());
        assertThrows(ConflictoException.class, () -> p.cancelar(MotivoCancelacion.CANCELACION_CLIENTE));
    }
}
