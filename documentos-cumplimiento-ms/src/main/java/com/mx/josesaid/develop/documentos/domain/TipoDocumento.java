package com.mx.josesaid.develop.documentos.domain;

/** Tipos de documento y su retención mínima desde la carga. Confirmada por Legal: contrato 10, permiso 5, registro legal 10 años. */
public enum TipoDocumento {
    CONTRATO(10),
    PERMISO(5),
    REGISTRO_LEGAL(10);

    private final int retencionAnios;

    TipoDocumento(int retencionAnios) {
        this.retencionAnios = retencionAnios;
    }

    /** Años que el documento debe conservarse antes de poder eliminarse. */
    public int retencionAnios() {
        return retencionAnios;
    }
}
