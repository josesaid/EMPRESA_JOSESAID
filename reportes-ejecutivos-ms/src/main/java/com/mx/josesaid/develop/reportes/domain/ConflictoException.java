package com.mx.josesaid.develop.reportes.domain;

/** Conflicto con el estado actual del reporte. HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
