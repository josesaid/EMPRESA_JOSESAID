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
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;

/**
 * API de reportes. Casi toda la API es de DIRECCION; la consulta también acepta JEFE_AREA, limitada a su área. La carga principal de indicadores es por Kafka;
 * POST /indicadores existe para cargas manuales y correcciones.
 */
@Validated
@RestController
@RequestMapping("/v1")
public class ReportesControlador {

    private final ReportesService servicio;

    /** Recibe el servicio de reportes. El controlador no contiene reglas de negocio. */
    public ReportesControlador(ReportesService servicio) {
        this.servicio = servicio;
    }

    /** Registra un indicador de un área para un periodo. Si el periodo no existe, lo abre. Un periodo cerrado responde 409. Solo puede hacerlo DIRECCION. */
    @PostMapping("/indicadores")
    @PreAuthorize("hasRole('DIRECCION')")
    public ResponseEntity<IndicadorRespuesta> registrarIndicador(@Valid @RequestBody RegistrarIndicadorPeticion p) {
        Indicador i = servicio.registrarIndicador(p.area(), p.nombre(), Periodo.de(p.periodo()),
                p.valor(), p.fuenteActualizadaUtc());
        return ResponseEntity.status(HttpStatus.CREATED).body(IndicadorRespuesta.de(i, false));
    }

    /** DIRECCION consulta cualquier área. JEFE_AREA solo la suya, según el claim {@code area} del token. */
    @GetMapping("/indicadores")
    @PreAuthorize("hasAnyRole('DIRECCION','JEFE_AREA')")
    public List<IndicadorRespuesta> consultarIndicadores(@RequestParam(required = false) Area area,
                                                         @RequestParam @Size(max = 20) String periodo,
                                                         Authentication autenticacion) {
        return servicio.consultarIndicadores(alcance(area, autenticacion), Periodo.de(periodo)).stream()
                .map(v -> IndicadorRespuesta.de(v.indicador(), v.desactualizado()))
                .toList();
    }

    /** Limita el área consultada a la del usuario, salvo que sea de dirección. Así cada área solo ve sus indicadores. */
    private static Area alcance(Area solicitada, Authentication autenticacion) {
        boolean direccion = autenticacion.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DIRECCION".equals(a.getAuthority()));
        if (direccion) {
            return solicitada;
        }
        String claim = autenticacion instanceof JwtAuthenticationToken jwt
                ? jwt.getToken().getClaimAsString("area") : null;
        Area propia;
        try {
            propia = claim == null ? null : Area.valueOf(claim);
        } catch (IllegalArgumentException e) {
            propia = null;
        }
        if (propia == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario no tiene un área válida asignada");
        }
        if (solicitada != null && solicitada != propia) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puede consultar indicadores de su área");
        }
        return propia;
    }

    /** DIRECCION ve el reporte completo. JEFE_AREA ve solo los indicadores de su área. */
    @GetMapping("/reportes/{periodo}")
    @PreAuthorize("hasAnyRole('DIRECCION','JEFE_AREA')")
    public ReporteRespuesta consultarReporte(@PathVariable String periodo, Authentication autenticacion) {
        ReportesService.ReporteVista v = servicio.consultarReporte(Periodo.de(periodo));
        Area alcance = alcance(null, autenticacion);
        List<IndicadorConsolidado> indicadores = alcance == null ? v.indicadores()
                : v.indicadores().stream().filter(i -> i.area() == alcance).toList();
        return ReporteRespuesta.de(v.reporte(), indicadores);
    }

    /** Cierra el reporte del periodo: consolida los indicadores y no admite más cambios. Solo puede hacerlo DIRECCION. */
    @PostMapping("/reportes/{periodo}/cierre")
    @PreAuthorize("hasRole('DIRECCION')")
    public ReporteRespuesta cerrarReporte(@PathVariable String periodo) {
        servicio.cerrarReporte(Periodo.de(periodo));
        ReportesService.ReporteVista v = servicio.consultarReporte(Periodo.de(periodo));
        return ReporteRespuesta.de(v.reporte(), v.indicadores());
    }

    /** Exporta el reporte en CSV, PDF o XLSX. Devuelve el archivo con su nombre y tipo MIME. */
    @GetMapping("/reportes/{id}/exportacion")
    @PreAuthorize("hasRole('DIRECCION')")
    public ResponseEntity<byte[]> exportar(@PathVariable UUID id, @RequestParam(defaultValue = "CSV") @Size(max = 10) String formato) {
        ReportesService.Exportacion e = servicio.exportar(id, formato);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + e.nombreArchivo() + "\"")
                .contentType(MediaType.parseMediaType(e.formato().mediaType()))
                .body(e.contenido());
    }

    // ---- Peticiones ----

    /**
     * Cuerpo para registrar un indicador: área, nombre, periodo, valor y fecha de la fuente.
     *
     * @param area área de negocio a la que pertenece, de tipo {@code Area}.
     * @param nombre nombre descriptivo, de tipo {@code String}.
     * @param periodo periodo de referencia (por ejemplo, año y mes), de tipo {@code String}.
     * @param valor valor registrado, de tipo {@code BigDecimal}.
     * @param fuenteActualizadaUtc fecha y hora (UTC) de la última actualización de la fuente, de tipo {@code Instant}.
     */
    public record RegistrarIndicadorPeticion(
            @NotNull Area area,
            @NotBlank String nombre,
            @NotBlank String periodo,
            @NotNull BigDecimal valor,
            @NotNull Instant fuenteActualizadaUtc) {
    }

    // ---- Respuestas ----

    /**
     * Indicador tal como lo ve el cliente. Marca si la fuente está desactualizada.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param area área de negocio a la que pertenece, de tipo {@code Area}.
     * @param nombre nombre descriptivo, de tipo {@code String}.
     * @param valor valor registrado, de tipo {@code BigDecimal}.
     * @param periodo periodo de referencia (por ejemplo, año y mes), de tipo {@code String}.
     * @param fuenteActualizadaUtc fecha y hora (UTC) de la última actualización de la fuente, de tipo {@code Instant}.
     * @param desactualizado desactualizado, de tipo {@code boolean}.
     */
    public record IndicadorRespuesta(UUID id, Area area, String nombre, BigDecimal valor, String periodo,
                                     Instant fuenteActualizadaUtc, boolean desactualizado) {
        /** Convierte un indicador del dominio en su respuesta. */
        static IndicadorRespuesta de(Indicador i, boolean desactualizado) {
            return new IndicadorRespuesta(i.id(), i.area(), i.nombre(), i.valor(), i.periodo().clave(),
                    i.fuenteActualizadaUtc(), desactualizado);
        }
    }

    /**
     * Indicador consolidado de un reporte cerrado.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param area área de negocio a la que pertenece, de tipo {@code Area}.
     * @param nombre nombre descriptivo, de tipo {@code String}.
     * @param valor valor registrado, de tipo {@code BigDecimal}.
     * @param fuenteActualizadaUtc fecha y hora (UTC) de la última actualización de la fuente, de tipo {@code Instant}.
     * @param desactualizado desactualizado, de tipo {@code boolean}.
     */
    public record IndicadorConsolidadoRespuesta(UUID id, Area area, String nombre, BigDecimal valor,
                                                Instant fuenteActualizadaUtc, boolean desactualizado) {
        /** Convierte un indicador consolidado del dominio en su respuesta. */
        static IndicadorConsolidadoRespuesta de(IndicadorConsolidado c) {
            return new IndicadorConsolidadoRespuesta(c.id(), c.area(), c.nombre(), c.valor(),
                    c.fuenteActualizadaUtc(), c.desactualizado());
        }
    }

    /**
     * Reporte con sus indicadores. Si está cerrado, incluye los consolidados.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param periodo periodo de referencia (por ejemplo, año y mes), de tipo {@code String}.
     * @param estatus estado actual del registro, de tipo {@code EstatusReporte}.
     * @param creadoUtc creado utc, de tipo {@code Instant}.
     * @param cerradoUtc cerrado utc, de tipo {@code Instant}.
     * @param indicadores indicadores, de tipo {@code List<IndicadorConsolidadoRespuesta>}.
     */
    public record ReporteRespuesta(UUID id, String periodo, EstatusReporte estatus, Instant creadoUtc,
                                   Instant cerradoUtc, List<IndicadorConsolidadoRespuesta> indicadores) {
        /** Convierte un reporte del dominio en su respuesta. */
        static ReporteRespuesta de(Reporte r, List<IndicadorConsolidado> indicadores) {
            return new ReporteRespuesta(r.getId(), r.getPeriodo().clave(), r.getEstatus(), r.getCreadoUtc(),
                    r.getCerradoUtc(), indicadores.stream().map(IndicadorConsolidadoRespuesta::de).toList());
        }
    }
}
