package com.mx.josesaid.develop.contabilidad.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mx.josesaid.develop.contabilidad.domain.ConflictoException;
import com.mx.josesaid.develop.contabilidad.domain.CuentaContable;
import com.mx.josesaid.develop.contabilidad.domain.CuentaRepositorio;
import com.mx.josesaid.develop.contabilidad.domain.Movimiento;
import com.mx.josesaid.develop.contabilidad.domain.Naturaleza;
import com.mx.josesaid.develop.contabilidad.domain.Periodo;
import com.mx.josesaid.develop.contabilidad.domain.PeriodoRepositorio;
import com.mx.josesaid.develop.contabilidad.domain.Poliza;
import com.mx.josesaid.develop.contabilidad.domain.PolizaRepositorio;
import com.mx.josesaid.develop.contabilidad.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.contabilidad.domain.TipoPoliza;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ContabilidadServiceTest {

    private final Map<UUID, CuentaContable> cuentas = new HashMap<>();
    private final Map<YearMonth, Periodo> periodos = new HashMap<>();
    private final Map<UUID, Poliza> polizas = new HashMap<>();
    private final List<EventoDominio> eventos = new ArrayList<>();
    private ContabilidadService servicio;

    @BeforeEach
    void preparar() {
        CuentaRepositorio cuentaRepo = new CuentaRepositorio() {
            public CuentaContable guardar(CuentaContable c) { cuentas.put(c.getId(), c); return c; }
            public Optional<CuentaContable> buscarPorId(UUID id) { return Optional.ofNullable(cuentas.get(id)); }
            public boolean existePorCodigo(String codigo) {
                return cuentas.values().stream().anyMatch(c -> c.getCodigo().equals(codigo));
            }
            public List<CuentaContable> listarActivas() {
                return cuentas.values().stream().filter(CuentaContable::isActiva)
                        .sorted(java.util.Comparator.comparing(CuentaContable::getCodigo)).toList();
            }
        };
        PeriodoRepositorio periodoRepo = new PeriodoRepositorio() {
            public Periodo guardar(Periodo p) { periodos.put(p.getAnioMes(), p); return p; }
            public Optional<Periodo> buscarPorAnioMes(YearMonth ym) { return Optional.ofNullable(periodos.get(ym)); }
        };
        PolizaRepositorio polizaRepo = new PolizaRepositorio() {
            public Poliza guardar(Poliza p) { polizas.put(p.getId(), p); return p; }
            public Optional<Poliza> buscarPorId(UUID id) { return Optional.ofNullable(polizas.get(id)); }
            public List<Poliza> buscarPorPeriodo(YearMonth ym) {
                return polizas.values().stream().filter(p -> p.getPeriodo().equals(ym)).toList();
            }
        };
        Clock reloj = Clock.fixed(Instant.parse("2026-10-20T10:00:00Z"), ZoneOffset.UTC);
        servicio = new ContabilidadService(cuentaRepo, periodoRepo, polizaRepo, eventos::add, reloj);
    }

    @Test
    void noSePermitenCodigosDeCuentaDuplicados() {
        servicio.crearCuenta("1101", "Caja", Naturaleza.DEUDORA);
        assertThatThrownBy(() -> servicio.crearCuenta("1101", "Otra", Naturaleza.DEUDORA))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void polizaDescuadradaNoSeGuardaNiPublicaEvento() {
        UUID caja = servicio.crearCuenta("1101", "Caja", Naturaleza.DEUDORA).getId();
        UUID ventas = servicio.crearCuenta("4101", "Ventas", Naturaleza.ACREEDORA).getId();
        List<Movimiento> mal = List.of(
                new Movimiento(caja, new BigDecimal("100"), null),
                new Movimiento(ventas, null, new BigDecimal("50")));

        assertThatThrownBy(() -> servicio.registrarPoliza(TipoPoliza.INGRESO, LocalDate.of(2026, 10, 1), mal))
                .isInstanceOf(ReglaDeNegocioException.class);
        assertThat(polizas).isEmpty();
        assertThat(eventos).isEmpty();
    }

    @Test
    void periodoCerradoRechazaPolizasYCierreEmiteEvento() {
        UUID caja = servicio.crearCuenta("1101", "Caja", Naturaleza.DEUDORA).getId();
        UUID ventas = servicio.crearCuenta("4101", "Ventas", Naturaleza.ACREEDORA).getId();
        List<Movimiento> ok = List.of(
                new Movimiento(caja, new BigDecimal("100"), null),
                new Movimiento(ventas, null, new BigDecimal("100")));
        servicio.registrarPoliza(TipoPoliza.INGRESO, LocalDate.of(2026, 10, 1), ok);
        assertThat(eventos).extracting(EventoDominio::tipo).containsExactly(ContabilidadService.EVENTO_POLIZA_APLICADA);

        servicio.cerrarPeriodo(YearMonth.of(2026, 10));
        assertThat(eventos).extracting(EventoDominio::tipo).contains(ContabilidadService.EVENTO_PERIODO_CERRADO);
        assertThatThrownBy(() -> servicio.registrarPoliza(TipoPoliza.INGRESO, LocalDate.of(2026, 10, 2), ok))
                .isInstanceOf(ConflictoException.class);
    }

    @Test
    void balanzaDelPeriodoCuadra() {
        UUID caja = servicio.crearCuenta("1101", "Caja", Naturaleza.DEUDORA).getId();
        UUID ventas = servicio.crearCuenta("4101", "Ventas", Naturaleza.ACREEDORA).getId();
        servicio.registrarPoliza(TipoPoliza.INGRESO, LocalDate.of(2026, 10, 1), List.of(
                new Movimiento(caja, new BigDecimal("250"), null),
                new Movimiento(ventas, null, new BigDecimal("250"))));

        assertThat(servicio.estadosFinancieros(YearMonth.of(2026, 10)).cuadra()).isTrue();
    }
}
