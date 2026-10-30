package com.mx.josesaid.develop.reportes.domain;

/** Áreas que reportan indicadores. Cada área es dueña de sus datos; este servicio solo consolida. */
public enum Area {
    VENTAS_ATENCION_CLIENTES("Ventas y atención a clientes"),
    FINANZAS_CONTABILIDAD("Finanzas y contabilidad"),
    OPERACIONES("Operaciones"),
    COMPRAS_PROVEEDORES("Compras y proveedores"),
    ADMINISTRACION_LEGAL("Administración y legal"),
    DIRECCION_GENERAL("Dirección general");

    private final String etiqueta;

    Area(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
