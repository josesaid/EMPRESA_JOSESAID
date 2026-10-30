package com.mx.josesaid.develop.reportes.infrastructure.exportacion;

import com.mx.josesaid.develop.reportes.application.DatosExportacion;
import com.mx.josesaid.develop.reportes.application.ExportadorReporte;
import com.mx.josesaid.develop.reportes.application.FormatoExportacion;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

/** Libro Excel (.xlsx) con una hoja, encabezados en negrita y panel congelado. */
@Component
public class ExportadorXlsx implements ExportadorReporte {

    private static final String[] CABECERAS = {
            "Área", "Indicador", "Periodo", "Valor", "Fuente actualizada (UTC)", "Desactualizado"};

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.XLSX;
    }

    @Override
    public byte[] exportar(DatosExportacion datos) {
        try (XSSFWorkbook libro = new XSSFWorkbook();
             ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Reporte " + datos.periodo());

            Font negrita = libro.createFont();
            negrita.setBold(true);
            CellStyle estiloCabecera = libro.createCellStyle();
            estiloCabecera.setFont(negrita);

            Row cabecera = hoja.createRow(0);
            for (int i = 0; i < CABECERAS.length; i++) {
                var celda = cabecera.createCell(i);
                celda.setCellValue(CABECERAS[i]);
                celda.setCellStyle(estiloCabecera);
                hoja.setColumnWidth(i, 22 * 256);
            }

            int fila = 1;
            for (IndicadorConsolidado c : datos.indicadores()) {
                Row r = hoja.createRow(fila++);
                r.createCell(0).setCellValue(c.area().etiqueta());
                r.createCell(1).setCellValue(c.nombre());
                r.createCell(2).setCellValue(datos.periodo());
                r.createCell(3).setCellValue(c.valor().doubleValue());
                r.createCell(4).setCellValue(c.fuenteActualizadaUtc().toString());
                r.createCell(5).setCellValue(c.desactualizado() ? "Sí" : "No");
            }
            hoja.createFreezePane(0, 1);

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo generar el XLSX del reporte " + datos.reporteId(), e);
        }
    }
}
