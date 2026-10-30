package com.mx.josesaid.develop.reportes.infrastructure.exportacion;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.mx.josesaid.develop.reportes.application.DatosExportacion;
import com.mx.josesaid.develop.reportes.application.ExportadorReporte;
import com.mx.josesaid.develop.reportes.application.FormatoExportacion;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

/** PDF horizontal A4 con título, identificador del reporte y tabla de indicadores. */
@Component
public class ExportadorPdf implements ExportadorReporte {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ISO_INSTANT;

    @Override
    public FormatoExportacion formato() {
        return FormatoExportacion.PDF;
    }

    @Override
    public byte[] exportar(DatosExportacion datos) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A4.rotate());
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();
            documento.add(new Paragraph("Reporte ejecutivo " + datos.periodo(),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            documento.add(new Paragraph("Identificador: " + datos.reporteId(),
                    FontFactory.getFont(FontFactory.HELVETICA, 9)));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(new float[]{2f, 4f, 2f, 3f, 2f});
            tabla.setWidthPercentage(100f);
            for (String cabecera : new String[]{"Área", "Indicador", "Valor", "Fuente actualizada (UTC)", "Estado"}) {
                tabla.addCell(new PdfPCell(new Phrase(cabecera, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10))));
            }
            for (IndicadorConsolidado c : datos.indicadores()) {
                tabla.addCell(celda(c.area().etiqueta()));
                tabla.addCell(celda(c.nombre()));
                tabla.addCell(celda(c.valor().toPlainString()));
                tabla.addCell(celda(FECHA.format(c.fuenteActualizadaUtc())));
                tabla.addCell(celda(c.desactualizado() ? "Desactualizado" : "Al día"));
            }
            documento.add(tabla);
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del reporte " + datos.reporteId(), e);
        } finally {
            documento.close();
        }
        return salida.toByteArray();
    }

    private static PdfPCell celda(String texto) {
        return new PdfPCell(new Phrase(texto, FontFactory.getFont(FontFactory.HELVETICA, 9)));
    }
}
