package com.mx.josesaid.develop.reportes.infrastructure.exportacion;

import com.mx.josesaid.develop.reportes.application.DatosExportacion;
import com.mx.josesaid.develop.reportes.application.ExportadorReporte;
import com.mx.josesaid.develop.reportes.application.FormatoExportacion;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/** CSV en UTF-8 con BOM para que Excel lo abra con acentos correctos. */
@Component
public class ExportadorCsv implements ExportadorReporte {

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.CSV;
    }

    @Override
    public byte[] exportar(DatosExportacion datos) {
        StringBuilder csv = new StringBuilder("﻿")
                .append("area,nombre,periodo,valor,fuente_actualizada_utc,desactualizado\n");
        for (IndicadorConsolidado c : datos.indicadores()) {
            csv.append(escapar(c.area().name())).append(',')
                    .append(escapar(c.nombre())).append(',')
                    .append(escapar(datos.periodo())).append(',')
                    .append(c.valor().toPlainString()).append(',')
                    .append(c.fuenteActualizadaUtc()).append(',')
                    .append(c.desactualizado()).append('\n');
        }
        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String escapar(String valor) {
        return "\"" + valor.replace("\"", "\"\"") + "\"";
    }
}
