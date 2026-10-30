package com.mx.josesaid.develop.documentos.domain;

/** Tipos de documento. Los años de retención son propuesta pendiente de confirmación legal. */
public enum TipoDocumento {
    CONTRATO(10),
    PERMISO(5),
    REGISTRO_LEGAL(10);

    private final int retencionAnios;

    TipoDocumento(int retencionAnios) {
        this.retencionAnios = retencionAnios;
    }

    public int retencionAnios() {
        return retencionAnios;
    }
}
