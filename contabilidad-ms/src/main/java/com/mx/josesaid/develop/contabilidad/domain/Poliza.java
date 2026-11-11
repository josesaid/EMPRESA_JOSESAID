package com.mx.josesaid.develop.contabilidad.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;

/** Póliza contable (asiento). Debe cuadrar: suma de cargos igual a suma de abonos. */
public class Poliza {

    private final UUID id;
    private final YearMonth periodo;
    private final TipoPoliza tipo;
    private final LocalDate fecha;
    private EstatusPoliza estatus;
    private final UUID reversaDe;
    private final List<Movimiento> movimientos;

    /** Constructor privado: las pólizas se crean con sus fábricas. */
    private Poliza(UUID id, YearMonth periodo, TipoPoliza tipo, LocalDate fecha, EstatusPoliza estatus,
                   UUID reversaDe, List<Movimiento> movimientos) {
        this.id = id;
        this.periodo = periodo;
        this.tipo = tipo;
        this.fecha = fecha;
        this.estatus = estatus;
        this.reversaDe = reversaDe;
        this.movimientos = new ArrayList<>(movimientos);
    }

    /** Valida el cuadre y crea la póliza aplicada. */
    public static Poliza aplicar(TipoPoliza tipo, LocalDate fecha, List<Movimiento> movimientos) {
        if (tipo == null) {
            throw new ReglaDeNegocioException("El tipo de póliza es obligatorio");
        }
        if (fecha == null) {
            throw new ReglaDeNegocioException("La fecha de la póliza es obligatoria");
        }
        if (movimientos == null || movimientos.size() < 2) {
            throw new ReglaDeNegocioException("La póliza requiere al menos dos movimientos");
        }
        BigDecimal cargos = movimientos.stream().map(Movimiento::getCargo).reduce(ZERO, BigDecimal::add);
        BigDecimal abonos = movimientos.stream().map(Movimiento::getAbono).reduce(ZERO, BigDecimal::add);
        if (cargos.compareTo(abonos) != 0) {
            throw new ReglaDeNegocioException("La póliza no cuadra: cargos " + cargos.toPlainString()
                    + " y abonos " + abonos.toPlainString());
        }
        return new Poliza(UUID.randomUUID(), YearMonth.from(fecha), tipo, fecha, EstatusPoliza.APLICADA, null,
                movimientos);
    }

    /** Crea la póliza de reversa con los movimientos invertidos y marca esta póliza como REVERSADA. */
    public Poliza reversar(LocalDate fechaReversa) {
        if (estatus != EstatusPoliza.APLICADA) {
            throw new ConflictoException("Solo una póliza aplicada puede reversarse");
        }
        if (fechaReversa == null) {
            throw new ReglaDeNegocioException("La fecha de la reversa es obligatoria");
        }
        this.estatus = EstatusPoliza.REVERSADA;
        List<Movimiento> invertidos = movimientos.stream().map(Movimiento::invertir).toList();
        return new Poliza(UUID.randomUUID(), YearMonth.from(fechaReversa), tipo, fechaReversa,
                EstatusPoliza.APLICADA, id, invertidos);
    }

    /** Reconstruye desde la persistencia sin volver a validar. */
    public static Poliza reconstruir(UUID id, YearMonth periodo, TipoPoliza tipo, LocalDate fecha,
                                     EstatusPoliza estatus, UUID reversaDe, List<Movimiento> movimientos) {
        return new Poliza(id, periodo, tipo, fecha, estatus, reversaDe, movimientos);
    }

    /** Devuelve el total de la póliza: la suma de sus cargos. */
    public BigDecimal total() {
        return movimientos.stream().map(Movimiento::getCargo).reduce(ZERO, BigDecimal::add);
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el periodo de la póliza. */
    public YearMonth getPeriodo() { return periodo; }
    /** Devuelve el tipo de póliza. */
    public TipoPoliza getTipo() { return tipo; }
    /** Devuelve la fecha de la póliza. */
    public LocalDate getFecha() { return fecha; }
    /** Devuelve el estatus: aplicada o reversada. */
    public EstatusPoliza getEstatus() { return estatus; }
    /** Devuelve el id de la póliza que esta reversa anula, o nulo. */
    public UUID getReversaDe() { return reversaDe; }
    /** Devuelve los movimientos, en una vista de solo lectura. */
    public List<Movimiento> getMovimientos() { return Collections.unmodifiableList(movimientos); }
}
