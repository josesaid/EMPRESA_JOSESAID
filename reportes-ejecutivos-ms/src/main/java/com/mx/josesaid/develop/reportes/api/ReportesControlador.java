package com.mx.josesaid.develop.reportes.api;

import com.mx.josesaid.develop.reportes.application.ReportesService;
import com.mx.josesaid.develop.reportes.domain.Area;
import com.mx.josesaid.develop.reportes.domain.EstatusReporte;
import com.mx.josesaid.develop.reportes.domain.Indicador;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import com.mx.josesaid.develop.reportes.domain.Periodo;
import com.mx.josesaid.develop.reportes.domain.Reporte;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de reportes. Toda la API es de DIRECCION. La carga principal de indicadores es por Kafka;
 * POST /indicadores existe para cargas manuales y correcciones.
 */
@RestController
@RequestMapping("/v1")
public class ReportesControlador {

    private final ReportesService servicio;

    public ReportesControlador(ReportesService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/indicadores")
    @PreAuthorize("hasRole('DIRECCION')")
    public ResponseEntity<IndicadorRespuesta> registrarIndicador(@Valid @RequestBody RegistrarIndicadorPeticion p) {
        Indicador i = servicio.registrarIndicador(p.area(), p.nombre(), Periodo.de(p.periodo()),
                p.valor(), p.fuenteActualizadaUtc());
        return ResponseEntity.status(HttpStatus.CREATED).body(IndicadorRespuesta.de(i, false));
    }

    @GetMapping("/indicadores")
    @PreAuthorize("hasRole('DIRECCION')")
    public List<IndicadorRespuesta> consultarIndicadores(@RequestParam(required = false) Area area,
                                                         @RequestParam String periodo) {
        return servicio.consultarIndicadores(area, Periodo.de(periodo)).stream()
                .map(v -> IndicadorRespuesta.de(v.indicador(), v.desactualizado()))
                .toList();
    }

    @GetMapping("/reportes/{periodo}")
    @PreAuthorize("hasRole('DIRECCION')")
    public ReporteRespuesta consultarReporte(@PathVariable String periodo) {
        ReportesService.ReporteVista v = servicio.consultarReporte(Periodo.de(periodo));
        return ReporteRespuesta.de(v.reporte(), v.indicadores());
    }

    @PostMapping("/reportes/{periodo}/cierre")
    @PreAuthorize("hasRole('DIRECCION')")
    public ReporteRespuesta cerrarReporte(@PathVariable String periodo) {
        servicio.cerrarReporte(Periodo.de(periodo));
        ReportesService.ReporteVista v = servicio.consultarReporte(Periodo.de(periodo));
        return ReporteRespuesta.de(v.reporte(), v.indicadores());
    }

    @GetMapping("/reportes/{id}/exportacion")
    @PreAuthorize("hasRole('DIRECCION')")
    public ResponseEntity<byte[]> exportar(@PathVariable UUID id, @RequestParam(defaultValue = "CSV") String formato) {
        ReportesService.Exportacion e = servicio.exportar(id, formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + e.nombreArchivo() + "\"")
                .contentType(MediaType.parseMediaType(e.formato().mediaType()))
                .body(e.contenido());
    }

    // ---- Peticiones ----

    public record RegistrarIndicadorPeticion(
            @NotNull Area area,
            @NotBlank String nombre,
            @NotBlank String periodo,
            @NotNull BigDecimal valor,
            @NotNull Instant fuenteActualizadaUtc) {
    }

    // ---- Respuestas ----

    public record IndicadorRespuesta(UUID id, Area area, String nombre, BigDecimal valor, String periodo,
                                     Instant fuenteActualizadaUtc, boolean desactualizado) {
        static IndicadorRespuesta de(Indicador i, boolean desactualizado) {
            return new IndicadorRespuesta(i.id(), i.area(), i.nombre(), i.valor(), i.periodo().clave(),
                    i.fuenteActualizadaUtc(), desactualizado);
        }
    }

    public record IndicadorConsolidadoRespuesta(UUID id, Area area, String nombre, BigDecimal valor,
                                                Instant fuenteActualizadaUtc, boolean desactualizado) {
        static IndicadorConsolidadoRespuesta de(IndicadorConsolidado c) {
            return new IndicadorConsolidadoRespuesta(c.id(), c.area(), c.nombre(), c.valor(),
                    c.fuenteActualizadaUtc(), c.desactualizado());
        }
    }

    public record ReporteRespuesta(UUID id, String periodo, EstatusReporte estatus, Instant creadoUtc,
                                   Instant cerradoUtc, List<IndicadorConsolidadoRespuesta> indicadores) {
        static ReporteRespuesta de(Reporte r, List<IndicadorConsolidado> indicadores) {
            return new ReporteRespuesta(r.getId(), r.getPeriodo().clave(), r.getEstatus(), r.getCreadoUtc(),
                    r.getCerradoUtc(), indicadores.stream().map(IndicadorConsolidadoRespuesta::de).toList());
        }
    }
}
