package com.mx.josesaid.develop.reportes.application;

/** Puerto de exportación. Cada formato tiene su implementación en infrastructure/exportacion. */
public interface ExportadorReporte {

    FormatoExportacion formato();

    byte[] exportar(DatosExportacion datos);
}
