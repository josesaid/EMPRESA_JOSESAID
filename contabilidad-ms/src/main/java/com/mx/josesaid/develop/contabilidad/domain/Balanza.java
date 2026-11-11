package com.mx.josesaid.develop.contabilidad.domain;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

/**
 * Balanza de comprobación de un periodo. Solo incluye lo que sí está calculado:
 * balanza de comprobación. Estado de resultados y balance general son pendientes.
  * @param periodo periodo de referencia (por ejemplo, año y mes), de tipo {@code String}.
  * @param lineas líneas o renglones del documento, de tipo {@code List<Linea>}.
  * @param totalCargos total cargos, de tipo {@code BigDecimal}.
  * @param totalAbonos total abonos, de tipo {@code BigDecimal}.
  * @param cuadra cuadra, de tipo {@code boolean}.
 */
public record Balanza(String periodo, List<Linea> lineas, BigDecimal totalCargos, BigDecimal totalAbonos,
                      boolean cuadra) {

    /**
     * Línea de la balanza: cuenta, cargos, abonos y saldo según la naturaleza de la cuenta.
     *
     * @param cuentaId identificador de la cuenta contable, de tipo {@code UUID}.
     * @param codigo código legible que identifica el registro, de tipo {@code String}.
     * @param nombre nombre descriptivo, de tipo {@code String}.
     * @param cargos cargos, de tipo {@code BigDecimal}.
     * @param abonos abonos, de tipo {@code BigDecimal}.
     * @param saldo saldo pendiente, de tipo {@code BigDecimal}.
     */
    public record Linea(UUID cuentaId, String codigo, String nombre, BigDecimal cargos, BigDecimal abonos,
                        BigDecimal saldo) {
    }

    /** Suma cargos y abonos por cuenta. El saldo sigue la naturaleza de cada cuenta. */
    public static Balanza calcular(YearMonth periodo, List<Poliza> polizas, Map<UUID, CuentaContable> cuentas) {
        Map<UUID, BigDecimal[]> acumulado = new TreeMap<>();
        for (Poliza p : polizas) {
            for (Movimiento m : p.getMovimientos()) {
                BigDecimal[] t = acumulado.computeIfAbsent(m.getCuentaId(), k -> new BigDecimal[]{ZERO, ZERO});
                t[0] = t[0].add(m.getCargo());
                t[1] = t[1].add(m.getAbono());
            }
        }
        List<Linea> lineas = new ArrayList<>();
        BigDecimal totalCargos = ZERO;
        BigDecimal totalAbonos = ZERO;
        for (Map.Entry<UUID, BigDecimal[]> e : acumulado.entrySet()) {
            CuentaContable c = cuentas.get(e.getKey());
            if (c == null) {
                throw new IllegalStateException("Cuenta sin datos para la balanza: " + e.getKey());
            }
            BigDecimal cargos = e.getValue()[0];
            BigDecimal abonos = e.getValue()[1];
            BigDecimal saldo = c.getNaturaleza() == Naturaleza.DEUDORA ? cargos.subtract(abonos) : abonos.subtract(cargos);
            lineas.add(new Linea(c.getId(), c.getCodigo(), c.getNombre(), cargos, abonos, saldo));
            totalCargos = totalCargos.add(cargos);
            totalAbonos = totalAbonos.add(abonos);
        }
        return new Balanza(periodo.toString(), lineas, totalCargos, totalAbonos,
                totalCargos.compareTo(totalAbonos) == 0);
    }
}
