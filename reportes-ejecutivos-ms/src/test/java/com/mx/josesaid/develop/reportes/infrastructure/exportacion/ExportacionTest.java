package com.mx.josesaid.develop.reportes.infrastructure.exportacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mx.josesaid.develop.reportes.application.DatosExportacion;
import com.mx.josesaid.develop.reportes.application.FormatoExportacion;
import com.mx.josesaid.develop.reportes.domain.Area;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import com.mx.josesaid.develop.reportes.domain.SolicitudInvalidaException;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExportacionTest {

    private static DatosExportacion datos() {
        UUID id = UUID.randomUUID();
        return new DatosExportacion(id, "2026-10", List.of(
                new IndicadorConsolidado(UUID.randomUUID(), Area.OPERACIONES, "Órdenes cerradas",
                        new BigDecimal("12.50"), Instant.parse("2026-10-29T10:00:00Z"), false),
                new IndicadorConsolidado(UUID.randomUUID(), Area.FINANZAS_CONTABILIDAD, "Cobranza \"neta\"",
                        new BigDecimal("3"), Instant.parse("2026-09-01T00:00:00Z"), true)));
    }

    @Test
    void csvTieneCabeceraYFilasConEscape() {
        String csv = new String(new ExportadorCsv().exportar(datos()), StandardCharsets.UTF_8);
        assertTrue(csv.contains("area,nombre,periodo,valor,fuente_actualizada_utc,desactualizado"));
        assertTrue(csv.contains("\"OPERACIONES\",\"Órdenes cerradas\",\"2026-10\",12.50"));
        assertTrue(csv.contains("\"Cobranza \"\"neta\"\"\""));
    }

    @Test
    void pdfEmpiezaConCabeceraDePdf() {
        byte[] pdf = new ExportadorPdf().exportar(datos());
        assertEquals("%PDF-", new String(pdf, 0, 5, StandardCharsets.US_ASCII));
        assertTrue(pdf.length > 1000);
    }

    @Test
    void xlsxTieneEncabezadosYValoresNumericos() throws Exception {
        byte[] xlsx = new ExportadorXlsx().exportar(datos());
        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(xlsx))) {
            var hoja = libro.getSheetAt(0);
            assertEquals("Área", hoja.getRow(0).getCell(0).getStringCellValue());
            assertEquals("Operaciones", hoja.getRow(1).getCell(0).getStringCellValue());
            assertEquals("Órdenes cerradas", hoja.getRow(1).getCell(1).getStringCellValue());
            assertEquals(12.5, hoja.getRow(1).getCell(3).getNumericCellValue());
            assertEquals("Sí", hoja.getRow(2).getCell(5).getStringCellValue());
        }
    }

    @Test
    void formatoEsInsensibleAMayusculasYRechazaOtros() {
        assertEquals(FormatoExportacion.PDF, FormatoExportacion.desde("pdf"));
        assertEquals(FormatoExportacion.XLSX, FormatoExportacion.desde("Xlsx"));
        assertThrows(SolicitudInvalidaException.class, () -> FormatoExportacion.desde("DOCX"));
        assertThrows(SolicitudInvalidaException.class, () -> FormatoExportacion.desde(null));
    }
}
