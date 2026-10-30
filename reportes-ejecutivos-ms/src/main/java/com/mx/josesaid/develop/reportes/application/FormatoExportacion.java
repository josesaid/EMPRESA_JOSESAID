package com.mx.josesaid.develop.reportes.application;

import com.mx.josesaid.develop.reportes.domain.SolicitudInvalidaException;

/** Formatos de exportación de un reporte. CSV es el valor por omisión. */
public enum FormatoExportacion {
    CSV("text/csv;charset=UTF-8", "csv"),
    PDF("application/pdf", "pdf"),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx");

    private final String mediaType;
    private final String extension;

    FormatoExportacion(String mediaType, String extension) {
        this.mediaType = mediaType;
        this.extension = extension;
    }

    public String mediaType() {
        return mediaType;
    }

    public String extension() {
        return extension;
    }

    /** Acepta el nombre sin distinguir mayúsculas. Otro valor es 400. */
    public static FormatoExportacion desde(String valor) {
        if (valor != null) {
            for (FormatoExportacion f : values()) {
                if (f.name().equalsIgnoreCase(valor.trim())) {
                    return f;
                }
            }
        }
        throw new SolicitudInvalidaException("Formato no soportado: " + valor + ". Disponibles: CSV, PDF, XLSX");
    }
}
