package com.mx.josesaid.develop.reportes.application;

import com.mx.josesaid.develop.reportes.domain.Area;
import com.mx.josesaid.develop.reportes.domain.EstatusReporte;
import com.mx.josesaid.develop.reportes.domain.Indicador;
import com.mx.josesaid.develop.reportes.domain.IndicadorConsolidado;
import com.mx.josesaid.develop.reportes.domain.IndicadorRepositorio;
import com.mx.josesaid.develop.reportes.domain.Periodo;
import com.mx.josesaid.develop.reportes.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.reportes.domain.Reporte;
import com.mx.josesaid.develop.reportes.domain.ReporteRepositorio;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consolidación de indicadores, cierre de reportes y exportación. */
@Service
public class ReportesService {

    private final IndicadorRepositorio indicadores;
    private final ReporteRepositorio reportes;
    private final PublicadorEventos eventos;
    private final Clock reloj;
    private final MeterRegistry metricas;
    private final Duration frescura;
    private final Map<FormatoExportacion, ExportadorReporte> exportadores = new EnumMap<>(FormatoExportacion.class);

    public ReportesService(IndicadorRepositorio indicadores, ReporteRepositorio reportes, PublicadorEventos eventos,
                           Clock reloj, MeterRegistry metricas,
                           @Value("${reportes.frescura-dias:7}") int frescuraDias,
                           List<ExportadorReporte> listaExportadores) {
        this.indicadores = indicadores;
        this.reportes = reportes;
        this.eventos = eventos;
        this.reloj = reloj;
        this.metricas = metricas;
        this.frescura = Duration.ofDays(frescuraDias);
        listaExportadores.forEach(e -> this.exportadores.put(e.formato(), e));
    }

    public record IndicadorVista(Indicador indicador, boolean desactualizado) {
    }

    public record ReporteVista(Reporte reporte, List<IndicadorConsolidado> indicadores) {
    }

    /** Resultado de exportar: contenido binario, formato y nombre de archivo sugerido. */
    public record Exportacion(byte[] contenido, FormatoExportacion formato, String nombreArchivo) {
    }

    @Transactional
    public Indicador registrarIndicador(Area area, String nombre, Periodo periodo, BigDecimal valor, Instant fuente) {
        Reporte reporte = reportes.buscarPorPeriodo(periodo).orElseGet(() -> Reporte.abrir(periodo, Instant.now(reloj)));
        reporte.exigirAbierto();
        reportes.guardar(reporte);
        Indicador guardado = indicadores.guardar(Indicador.nuevo(area, nombre, periodo, valor, fuente));
        metricas.counter("indicadores.recibidos").increment();
        return guardado;
    }

    @Transactional(readOnly = true)
    public List<IndicadorVista> consultarIndicadores(Area area, Periodo periodo) {
        Instant ahora = Instant.now(reloj);
        return indicadores.buscar(area, periodo).stream()
                .map(i -> new IndicadorVista(i, i.desactualizado(ahora, frescura)))
                .toList();
    }

    @Transactional(readOnly = true)
    public ReporteVista consultarReporte(Periodo periodo) {
        Reporte r = cargarPorPeriodo(periodo);
        return new ReporteVista(r, indicadoresDe(r));
    }

    @Transactional
    public Reporte cerrarReporte(Periodo periodo) {
        Reporte r = cargarPorPeriodo(periodo);
        Instant ahora = Instant.now(reloj);
        r.cerrar(indicadoresDe(r), ahora);
        Reporte guardado = reportes.guardar(r);
        metricas.counter("reportes.cerrados").increment();
        eventos.publicar(new EventoDominio("reporte-cerrado", guardado.getId().toString(), Map.of(
                "id", guardado.getId().toString(),
                "periodo", periodo.clave())));
        return guardado;
    }

    /** Exporta el reporte en CSV, PDF o XLSX. Un formato desconocido responde 400. */
    @Transactional(readOnly = true)
    public Exportacion exportar(UUID id, String formato) {
        FormatoExportacion f = FormatoExportacion.desde(formato);
        Reporte r = buscarReporte(id);
        byte[] contenido = exportadores.get(f).exportar(
                new DatosExportacion(r.getId(), r.getPeriodo().clave(), indicadoresDe(r)));
        String nombre = "reporte-" + r.getPeriodo().clave() + "." + f.extension();
        return new Exportacion(contenido, f, nombre);
    }

    @Transactional(readOnly = true)
    public Reporte buscarReporte(UUID id) {
        return reportes.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Reporte no encontrado: " + id));
    }

    /** Reporte cerrado: foto guardada. Reporte abierto: consolidado en vivo. */
    private List<IndicadorConsolidado> indicadoresDe(Reporte r) {
        if (r.getEstatus() == EstatusReporte.CERRADO) {
            return r.getIndicadoresCerrados();
        }
        Instant ahora = Instant.now(reloj);
        return indicadores.buscar(null, r.getPeriodo()).stream()
                .map(i -> new IndicadorConsolidado(i.id(), i.area(), i.nombre(), i.valor(),
                        i.fuenteActualizadaUtc(), i.desactualizado(ahora, frescura)))
                .toList();
    }

    private Reporte cargarPorPeriodo(Periodo periodo) {
        return reportes.buscarPorPeriodo(periodo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe reporte para el periodo " + periodo));
    }
}
