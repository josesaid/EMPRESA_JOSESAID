package com.mx.josesaid.develop.contabilidad.domain;

import java.math.BigDecimal;
import java.util.UUID;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_EVEN;

/** Un renglón de póliza: exactamente un cargo o un abono, ambos positivos o cero. */
public final class Movimiento {

    private final UUID cuentaId;
    private final BigDecimal cargo;
    private final BigDecimal abono;

    public Movimiento(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
        if (cuentaId == null) {
            throw new ReglaDeNegocioException("Cada movimiento requiere una cuenta");
        }
        BigDecimal c = (cargo == null ? ZERO : cargo).setScale(2, HALF_EVEN);
        BigDecimal a = (abono == null ? ZERO : abono).setScale(2, HALF_EVEN);
        if (c.signum() < 0 || a.signum() < 0) {
            throw new ReglaDeNegocioException("Cargos y abonos no pueden ser negativos");
        }
        boolean soloCargo = c.signum() > 0 && a.signum() == 0;
        boolean soloAbono = a.signum() > 0 && c.signum() == 0;
        if (!soloCargo && !soloAbono) {
            throw new ReglaDeNegocioException("Cada movimiento debe tener un cargo o un abono, no ambos ni ninguno");
        }
        this.cuentaId = cuentaId;
        this.cargo = c;
        this.abono = a;
    }

    /** Contrapartida para reversar una póliza: el cargo pasa a abono y viceversa. */
    public Movimiento invertir() {
        return new Movimiento(cuentaId, abono, cargo);
    }

    public UUID getCuentaId() { return cuentaId; }
    public BigDecimal getCargo() { return cargo; }
    public BigDecimal getAbono() { return abono; }
}
