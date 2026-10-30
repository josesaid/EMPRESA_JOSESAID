package com.mx.josesaid.develop.documentos.domain;

/** Tipos de documento. Los años de retención son propuesta pendiente de confirmación legal. */
public enum TipoDocumento {
    CONTRATO(5),
    PERMISO(5),
    REGISTRO_LEGAL(5);

    private final int retencionAnios;

    TipoDocumento(int retencionAnios) {
        this.retencionAnios = retencionAnios;
    }

    public int retencionAnios() {
        return retencionAnios;
    }
}
