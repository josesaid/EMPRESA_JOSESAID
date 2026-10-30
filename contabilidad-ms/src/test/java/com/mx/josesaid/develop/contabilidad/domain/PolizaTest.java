package com.mx.josesaid.develop.contabilidad.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PolizaTest {

    private static final UUID CAJA = UUID.randomUUID();
    private static final UUID VENTAS = UUID.randomUUID();
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    private static List<Movimiento> cuadrada() {
        return List.of(
                new Movimiento(CAJA, new BigDecimal("100"), null),
                new Movimiento(VENTAS, null, new BigDecimal("100")));
    }

    @Test
    void polizaCuadradaSeAplicaEnSuPeriodo() {
        Poliza p = Poliza.aplicar(TipoPoliza.INGRESO, FECHA, cuadrada());
        assertThat(p.getEstatus()).isEqualTo(EstatusPoliza.APLICADA);
        assertThat(p.getPeriodo()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(p.total()).isEqualByComparingTo("100.00");
    }

    @Test
    void polizaDescuadradaSeRechazaConMensaje() {
        List<Movimiento> mal = List.of(
                new Movimiento(CAJA, new BigDecimal("100"), null),
                new Movimiento(VENTAS, null, new BigDecimal("90")));
        assertThatThrownBy(() -> Poliza.aplicar(TipoPoliza.DIARIO, FECHA, mal))
                .isInstanceOf(ReglaDeNegocioException.class)
                .hasMessageContaining("no cuadra");
    }

    @Test
    void movimientoConCargoYAbonoEsInvalido() {
        assertThatThrownBy(() -> new Movimiento(CAJA, BigDecimal.ONE, BigDecimal.ONE))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThatThrownBy(() -> new Movimiento(CAJA, null, null))
                .isInstanceOf(ReglaDeNegocioException.class);
    }

    @Test
    void reversaInvierteMovimientosYMarcaLaOriginal() {
        Poliza original = Poliza.aplicar(TipoPoliza.INGRESO, FECHA, cuadrada());
        Poliza reversa = original.reversar(LocalDate.of(2026, 10, 20));

        assertThat(original.getEstatus()).isEqualTo(EstatusPoliza.REVERSADA);
        assertThat(reversa.getReversaDe()).isEqualTo(original.getId());
        assertThat(reversa.getMovimientos().get(0).getAbono()).isEqualByComparingTo("100.00");
        assertThat(reversa.getMovimientos().get(0).getCargo()).isEqualByComparingTo("0.00");
        assertThatThrownBy(() -> original.reversar(LocalDate.of(2026, 10, 21)))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void periodoCerradoNoSeCierraDosVeces() {
        Periodo p = Periodo.abrir(YearMonth.of(2026, 10));
        p.cerrar();
        assertThat(p.estaCerrado()).isTrue();
        assertThatThrownBy(p::cerrar).isInstanceOf(ConflictoException.class);
    }

    @Test
    void balanzaCalculaSaldoSegunNaturalezaYCuadra() {
        CuentaContable caja = CuentaContable.reconstruir(CAJA, "1101", "Caja", Naturaleza.DEUDORA, true);
        CuentaContable ventas = CuentaContable.reconstruir(VENTAS, "4101", "Ventas", Naturaleza.ACREEDORA, true);
        Poliza p = Poliza.aplicar(TipoPoliza.INGRESO, FECHA, cuadrada());

        Balanza b = Balanza.calcular(YearMonth.of(2026, 10), List.of(p),
                java.util.Map.of(CAJA, caja, VENTAS, ventas));

        assertThat(b.cuadra()).isTrue();
        assertThat(b.totalCargos()).isEqualByComparingTo(b.totalAbonos());
        Balanza.Linea lineaVentas = b.lineas().stream().filter(l -> l.codigo().equals("4101")).findFirst().orElseThrow();
        assertThat(lineaVentas.saldo()).isEqualByComparingTo("100.00");
    }
}
