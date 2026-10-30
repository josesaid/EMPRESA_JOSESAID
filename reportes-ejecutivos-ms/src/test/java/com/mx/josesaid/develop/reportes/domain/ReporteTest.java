package com.mx.josesaid.develop.reportes.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReporteTest {

    private static final Periodo OCTUBRE = Periodo.de("2026-10");
    private static final Instant AHORA = Instant.parse("2026-10-29T12:00:00Z");

    private static IndicadorConsolidado consolidado(boolean desactualizado) {
        return new IndicadorConsolidado(UUID.randomUUID(), Area.OPERACIONES, "Órdenes cerradas",
                BigDecimal.TEN, AHORA, desactualizado);
    }

    @Test
    void indicadorSinFuenteRecienteQuedaDesactualizado() {
        Indicador viejo = Indicador.nuevo(Area.FINANZAS_CONTABILIDAD, "Cartera vencida", OCTUBRE,
                BigDecimal.ONE, Instant.parse("2020-01-01T00:00:00Z"));
        Indicador fresco = Indicador.nuevo(Area.OPERACIONES, "Órdenes", OCTUBRE, BigDecimal.ONE, AHORA);
        assertTrue(viejo.desactualizado(AHORA, Duration.ofDays(7)));
        assertFalse(fresco.desactualizado(AHORA, Duration.ofDays(7)));
    }

    @Test
    void cerrarGuardaLaFotoYNoSePuedeVolverACerrar() {
        Reporte r = Reporte.abrir(OCTUBRE, AHORA);
        r.cerrar(List.of(consolidado(false)), AHORA);
        assertEquals(EstatusReporte.CERRADO, r.getEstatus());
        assertEquals(1, r.getIndicadoresCerrados().size());
        assertThrows(ConflictoException.class, () -> r.cerrar(List.of(consolidado(false)), AHORA));
    }

    @Test
    void unReporteCerradoNoAdmiteCambios() {
        Reporte r = Reporte.abrir(OCTUBRE, AHORA);
        r.cerrar(List.of(consolidado(true)), AHORA);
        assertThrows(ConflictoException.class, r::exigirAbierto);
    }

    @Test
    void noSeCierraUnPeriodoSinIndicadores() {
        Reporte r = Reporte.abrir(OCTUBRE, AHORA);
        assertThrows(ReglaDeNegocioException.class, () -> r.cerrar(List.of(), AHORA));
    }
}
