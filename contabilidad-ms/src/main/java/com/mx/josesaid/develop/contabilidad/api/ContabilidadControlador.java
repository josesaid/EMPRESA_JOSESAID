package com.mx.josesaid.develop.contabilidad.api;

import com.mx.josesaid.develop.contabilidad.application.ContabilidadService;
import com.mx.josesaid.develop.contabilidad.domain.Balanza;
import com.mx.josesaid.develop.contabilidad.domain.CuentaContable;
import com.mx.josesaid.develop.contabilidad.domain.EstatusPeriodo;
import com.mx.josesaid.develop.contabilidad.domain.EstatusPoliza;
import com.mx.josesaid.develop.contabilidad.domain.Movimiento;
import com.mx.josesaid.develop.contabilidad.domain.Naturaleza;
import com.mx.josesaid.develop.contabilidad.domain.Periodo;
import com.mx.josesaid.develop.contabilidad.domain.Poliza;
import com.mx.josesaid.develop.contabilidad.domain.TipoPoliza;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mx.josesaid.develop.contabilidad.infrastructure.idempotencia.Idempotencia;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;

/** Endpoints de contabilidad. CONTADOR escribe; DIRECCION solo consulta estados financieros y pólizas. */
@RestController
@RequestMapping("/v1")
public class ContabilidadControlador {

    private final ContabilidadService servicio;
    private final Idempotencia idempotencia;

    public ContabilidadControlador(ContabilidadService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    @PostMapping("/cuentas")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTADOR')")
    public CuentaRespuesta crearCuenta(@Valid @RequestBody CrearCuentaSolicitud solicitud) {
        return CuentaRespuesta.de(servicio.crearCuenta(solicitud.codigo(), solicitud.nombre(), solicitud.naturaleza()));
    }

    @PostMapping("/polizas")
    @PreAuthorize("hasRole('CONTADOR')")
    public ResponseEntity<?> registrarPoliza(@RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
                                             @Valid @RequestBody RegistrarPolizaSolicitud solicitud) {
        return idempotencia.ejecutar(clave, "POST /v1/polizas", solicitud, HttpStatus.CREATED, () -> {
            List<Movimiento> movimientos = solicitud.movimientos().stream()
                    .map(m -> new Movimiento(m.cuentaId(), m.cargo(), m.abono()))
                    .toList();
            return PolizaRespuesta.de(servicio.registrarPoliza(solicitud.tipo(), solicitud.fecha(), movimientos));
        });
    }

    @PostMapping("/polizas/{id}/reversa")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CONTADOR')")
    public PolizaRespuesta reversarPoliza(@PathVariable UUID id) {
        return PolizaRespuesta.de(servicio.reversarPoliza(id));
    }

    @GetMapping("/polizas/{id}")
    @PreAuthorize("hasAnyRole('CONTADOR','DIRECCION')")
    public PolizaRespuesta obtenerPoliza(@PathVariable UUID id) {
        return PolizaRespuesta.de(servicio.obtenerPoliza(id));
    }

    @PostMapping("/periodos/{anioMes}/cierre")
    @PreAuthorize("hasRole('CONTADOR')")
    public PeriodoRespuesta cerrarPeriodo(@PathVariable String anioMes) {
        return PeriodoRespuesta.de(servicio.cerrarPeriodo(YearMonth.parse(anioMes)));
    }

    @GetMapping("/estados-financieros")
    @PreAuthorize("hasAnyRole('CONTADOR','DIRECCION')")
    public Balanza estadosFinancieros(@RequestParam String periodo) {
        return servicio.estadosFinancieros(YearMonth.parse(periodo));
    }

    public record CrearCuentaSolicitud(
            @NotBlank @Size(max = 20) String codigo,
            @NotBlank @Size(max = 150) String nombre,
            @NotNull Naturaleza naturaleza) {
    }

    public record MovimientoSolicitud(
            @NotNull UUID cuentaId,
            @DecimalMin("0") BigDecimal cargo,
            @DecimalMin("0") BigDecimal abono) {
    }

    public record RegistrarPolizaSolicitud(
            @NotNull TipoPoliza tipo,
            @NotNull LocalDate fecha,
            @NotNull @Size(min = 2) List<@Valid MovimientoSolicitud> movimientos) {
    }

    public record CuentaRespuesta(UUID id, String codigo, String nombre, Naturaleza naturaleza, boolean activa) {
        static CuentaRespuesta de(CuentaContable c) {
            return new CuentaRespuesta(c.getId(), c.getCodigo(), c.getNombre(), c.getNaturaleza(), c.isActiva());
        }
    }

    public record MovimientoRespuesta(UUID cuentaId, BigDecimal cargo, BigDecimal abono) {
    }

    public record PolizaRespuesta(UUID id, String periodo, TipoPoliza tipo, LocalDate fecha, EstatusPoliza estatus,
                                  UUID reversaDe, BigDecimal total, List<MovimientoRespuesta> movimientos) {
        static PolizaRespuesta de(Poliza p) {
            return new PolizaRespuesta(p.getId(), p.getPeriodo().toString(), p.getTipo(), p.getFecha(),
                    p.getEstatus(), p.getReversaDe(), p.total(),
                    p.getMovimientos().stream()
                            .map(m -> new MovimientoRespuesta(m.getCuentaId(), m.getCargo(), m.getAbono()))
                            .toList());
        }
    }

    public record PeriodoRespuesta(String anioMes, EstatusPeriodo estatus) {
        static PeriodoRespuesta de(Periodo p) {
            return new PeriodoRespuesta(p.getAnioMes().toString(), p.getEstatus());
        }
    }
}
