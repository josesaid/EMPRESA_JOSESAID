package com.mx.josesaid.develop.contabilidad.domain;

import java.time.YearMonth;

/** Periodo contable mensual. Un periodo cerrado no admite pólizas nuevas ni reversas. */
public class Periodo {

    private final YearMonth anioMes;
    private EstatusPeriodo estatus;

    /** Constructor privado: los periodos se crean con sus fábricas. */
    private Periodo(YearMonth anioMes, EstatusPeriodo estatus) {
        this.anioMes = anioMes;
        this.estatus = estatus;
    }

    /** Abre un periodo nuevo en estatus abierto. */
    public static Periodo abrir(YearMonth anioMes) {
        return new Periodo(anioMes, EstatusPeriodo.ABIERTO);
    }

    /** Reconstruye un periodo desde la base de datos. */
    public static Periodo reconstruir(YearMonth anioMes, EstatusPeriodo estatus) {
        return new Periodo(anioMes, estatus);
    }

    /** Cierra el periodo. Un periodo ya cerrado no se vuelve a cerrar. */
    public void cerrar() {
        if (estatus == EstatusPeriodo.CERRADO) {
            throw new ConflictoException("El periodo " + anioMes + " ya está cerrado");
        }
        this.estatus = EstatusPeriodo.CERRADO;
    }

    /** Indica si el periodo está cerrado. */
    public boolean estaCerrado() {
        return estatus == EstatusPeriodo.CERRADO;
    }

    /** Devuelve el año y mes del periodo. */
    public YearMonth getAnioMes() { return anioMes; }
    /** Devuelve el estatus del periodo. */
    public EstatusPeriodo getEstatus() { return estatus; }
}
