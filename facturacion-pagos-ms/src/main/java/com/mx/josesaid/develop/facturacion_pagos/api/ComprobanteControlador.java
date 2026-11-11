package com.mx.josesaid.develop.facturacion_pagos.api;

import com.mx.josesaid.develop.facturacion_pagos.application.FacturacionService;
import com.mx.josesaid.develop.facturacion_pagos.domain.Comprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.EstatusCobro;
import com.mx.josesaid.develop.facturacion_pagos.domain.EstatusComprobante;
import com.mx.josesaid.develop.facturacion_pagos.domain.Pago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mx.josesaid.develop.facturacion_pagos.infrastructure.idempotencia.Idempotencia;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

/** Endpoints de comprobantes y pagos. Escribir exige FINANZAS; leer también permite VENTAS. */
@Validated
@RestController
@RequestMapping("/v1/comprobantes")
public class ComprobanteControlador {

    private final FacturacionService servicio;
    private final Idempotencia idempotencia;

    /** Recibe el servicio de facturación y el control de idempotencia. */
    public ComprobanteControlador(FacturacionService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    /** Crea un comprobante en borrador a partir de una orden. La tasa de IVA la fija el servidor según la fecha. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FINANZAS')")
    public ComprobanteRespuesta crear(@Valid @RequestBody CrearComprobanteSolicitud solicitud) {
        return ComprobanteRespuesta.de(servicio.crearBorrador(
                solicitud.ordenId(), solicitud.moneda(), solicitud.subtotal()));
    }

    /** Busca un comprobante por su folio. */
    @GetMapping(params = "folio")
    @PreAuthorize("hasAnyRole('FINANZAS','VENTAS')")
    public ComprobanteRespuesta buscarPorFolio(@RequestParam @Size(max = 50) String folio) {
        return ComprobanteRespuesta.de(servicio.buscarPorFolio(folio));
    }

    /** Últimos 15 comprobantes emitidos con saldo por cobrar, del más reciente al más antiguo. */
    @GetMapping("/pendientes-cobro")
    @PreAuthorize("hasAnyRole('FINANZAS','VENTAS')")
    public List<ComprobantePendienteRespuesta> listarPendientesCobro() {
        return servicio.listarPendientesDeCobro().stream()
                .map(c -> new ComprobantePendienteRespuesta(c.id(), c.folio(), c.moneda(), c.total(), c.saldo(),
                        c.estatusCobro()))
                .toList();
    }

    /** Devuelve un comprobante con sus pagos y su saldo. */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANZAS','VENTAS')")
    public ComprobanteRespuesta obtener(@PathVariable UUID id) {
        return ComprobanteRespuesta.de(servicio.obtener(id));
    }

    /** Emite el comprobante y le asigna su folio. */
    @PostMapping("/{id}/emision")
    @PreAuthorize("hasRole('FINANZAS')")
    public ComprobanteRespuesta emitir(@PathVariable UUID id) {
        return ComprobanteRespuesta.de(servicio.emitir(id));
    }

    /** Cancela un comprobante emitido. Requiere motivo. */
    @PostMapping("/{id}/cancelacion")
    @PreAuthorize("hasRole('FINANZAS')")
    public ComprobanteRespuesta cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarSolicitud solicitud) {
        return ComprobanteRespuesta.de(servicio.cancelar(id, solicitud.motivo()));
    }

    /** Registra un pago contra el saldo del comprobante. Acepta Idempotency-Key para reintentar sin duplicar el pago. */
    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<?> registrarPago(@RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody RegistrarPagoSolicitud solicitud) {
        return idempotencia.ejecutar(clave, "POST /v1/comprobantes/" + id + "/pagos", solicitud, HttpStatus.CREATED,
                () -> ComprobanteRespuesta.de(servicio.registrarPago(
                        id, solicitud.monto(), solicitud.fechaPago(), solicitud.referencia())));
    }

    /**
     * Datos para crear el borrador: orden, moneda y subtotal.
     *
     * @param ordenId identificador del orden, de tipo {@code UUID}.
     * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
     * @param subtotal subtotal antes de impuestos, de tipo {@code BigDecimal}.
     */
    public record CrearComprobanteSolicitud(
            @NotNull UUID ordenId,
            @NotBlank String moneda,
            @NotNull @Positive BigDecimal subtotal) {
    }

    /**
     * Motivo de la cancelación, de hasta 200 caracteres.
     *
     * @param motivo motivo del registro o de la acción, de tipo {@code String}.
     */
    public record CancelarSolicitud(@NotBlank @Size(max = 200) String motivo) {
    }

    /**
     * Datos de un pago: monto, fecha opcional y referencia.
     *
     * @param monto monto del movimiento, de tipo {@code BigDecimal}.
     * @param fechaPago fecha en que se realizó el pago, de tipo {@code LocalDate}.
     * @param referencia referencia externa o interna del documento, de tipo {@code String}.
     */
    public record RegistrarPagoSolicitud(
            @NotNull @Positive BigDecimal monto,
            LocalDate fechaPago,
            @Size(max = 100) String referencia) {
    }

    /**
     * Comprobante emitido con saldo pendiente de cobro.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param folio folio, de tipo {@code String}.
     * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
     * @param total importe total, de tipo {@code BigDecimal}.
     * @param saldo saldo pendiente, de tipo {@code BigDecimal}.
     * @param estatusCobro estatus cobro, de tipo {@code EstatusCobro}.
     */
    public record ComprobantePendienteRespuesta(UUID id, String folio, String moneda, BigDecimal total,
                                                BigDecimal saldo, EstatusCobro estatusCobro) {
    }

    /**
     * Respuesta de un pago.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param monto monto del movimiento, de tipo {@code BigDecimal}.
     * @param fechaPago fecha en que se realizó el pago, de tipo {@code LocalDate}.
     * @param referencia referencia externa o interna del documento, de tipo {@code String}.
     */
    public record PagoRespuesta(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {
        /** Convierte un pago en su respuesta. */
        static PagoRespuesta de(Pago p) {
            return new PagoRespuesta(p.getId(), p.getMonto(), p.getFechaPago(), p.getReferencia());
        }
    }

    /**
     * Respuesta de un comprobante, con sus pagos y su saldo.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param ordenId identificador del orden, de tipo {@code UUID}.
     * @param folio folio, de tipo {@code String}.
     * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
     * @param subtotal subtotal antes de impuestos, de tipo {@code BigDecimal}.
     * @param iva iva, de tipo {@code BigDecimal}.
     * @param total importe total, de tipo {@code BigDecimal}.
     * @param tasaIva tasa de IVA aplicable, de tipo {@code BigDecimal}.
     * @param estatus estado actual del registro, de tipo {@code EstatusComprobante}.
     * @param motivoCancelacion motivo cancelacion, de tipo {@code String}.
     * @param estatusCobro estatus cobro, de tipo {@code EstatusCobro}.
     * @param pagado pagado, de tipo {@code BigDecimal}.
     * @param saldo saldo pendiente, de tipo {@code BigDecimal}.
     * @param pagos pagos, de tipo {@code List<PagoRespuesta>}.
     */
    public record ComprobanteRespuesta(
            UUID id,
            UUID ordenId,
            String folio,
            String moneda,
            BigDecimal subtotal,
            BigDecimal iva,
            BigDecimal total,
            BigDecimal tasaIva,
            EstatusComprobante estatus,
            String motivoCancelacion,
            EstatusCobro estatusCobro,
            BigDecimal pagado,
            BigDecimal saldo,
            List<PagoRespuesta> pagos) {

        /** Convierte un comprobante en su respuesta. */
        static ComprobanteRespuesta de(Comprobante c) {
            return new ComprobanteRespuesta(c.getId(), c.getOrdenId(), c.getFolio(), c.getMoneda(),
                    c.getSubtotal(), c.getIva(), c.getTotal(), c.getTasaIva(), c.getEstatus(),
                    c.getMotivoCancelacion(), c.estatusCobro(), c.pagado(), c.saldo(),
                    c.getPagos().stream().map(PagoRespuesta::de).toList());
        }
    }
}
