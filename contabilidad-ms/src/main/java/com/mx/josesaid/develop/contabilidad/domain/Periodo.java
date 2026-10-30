package com.mx.josesaid.develop.contabilidad.domain;

import java.time.YearMonth;

/** Periodo contable mensual. Un periodo cerrado no admite pólizas nuevas ni reversas. */
public class Periodo {

    private final YearMonth anioMes;
    private EstatusPeriodo estatus;

    private Periodo(YearMonth anioMes, EstatusPeriodo estatus) {
        this.anioMes = anioMes;
        this.estatus = estatus;
    }

    public static Periodo abrir(YearMonth anioMes) {
        return new Periodo(anioMes, EstatusPeriodo.ABIERTO);
    }

    public static Periodo reconstruir(YearMonth anioMes, EstatusPeriodo estatus) {
        return new Periodo(anioMes, estatus);
    }

    public void cerrar() {
        if (estatus == EstatusPeriodo.CERRADO) {
            throw new ConflictoException("El periodo " + anioMes + " ya está cerrado");
        }
        this.estatus = EstatusPeriodo.CERRADO;
    }

    public boolean estaCerrado() {
        return estatus == EstatusPeriodo.CERRADO;
    }

    public YearMonth getAnioMes() { return anioMes; }
    public EstatusPeriodo getEstatus() { return estatus; }
}
