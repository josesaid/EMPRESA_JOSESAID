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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mx.josesaid.develop.facturacion_pagos.infrastructure.idempotencia.Idempotencia;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;

/** Endpoints de comprobantes y pagos. Escribir exige FINANZAS; leer también permite VENTAS. */
@RestController
@RequestMapping("/v1/comprobantes")
public class ComprobanteControlador {

    private final FacturacionService servicio;
    private final Idempotencia idempotencia;

    public ComprobanteControlador(FacturacionService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('FINANZAS')")
    public ComprobanteRespuesta crear(@Valid @RequestBody CrearComprobanteSolicitud solicitud) {
        return ComprobanteRespuesta.de(servicio.crearBorrador(
                solicitud.ordenId(), solicitud.moneda(), solicitud.subtotal(), solicitud.tasaIva()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('FINANZAS','VENTAS')")
    public ComprobanteRespuesta obtener(@PathVariable UUID id) {
        return ComprobanteRespuesta.de(servicio.obtener(id));
    }

    @PostMapping("/{id}/emision")
    @PreAuthorize("hasRole('FINANZAS')")
    public ComprobanteRespuesta emitir(@PathVariable UUID id) {
        return ComprobanteRespuesta.de(servicio.emitir(id));
    }

    @PostMapping("/{id}/cancelacion")
    @PreAuthorize("hasRole('FINANZAS')")
    public ComprobanteRespuesta cancelar(@PathVariable UUID id, @Valid @RequestBody CancelarSolicitud solicitud) {
        return ComprobanteRespuesta.de(servicio.cancelar(id, solicitud.motivo()));
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<?> registrarPago(@RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody RegistrarPagoSolicitud solicitud) {
        return idempotencia.ejecutar(clave, "POST /v1/comprobantes/" + id + "/pagos", solicitud, HttpStatus.CREATED,
                () -> ComprobanteRespuesta.de(servicio.registrarPago(
                        id, solicitud.monto(), solicitud.fechaPago(), solicitud.referencia())));
    }

    public record CrearComprobanteSolicitud(
            @NotNull UUID ordenId,
            @NotBlank String moneda,
            @NotNull @Positive BigDecimal subtotal,
            @NotNull @DecimalMin("0") @DecimalMax("1") BigDecimal tasaIva) {
    }

    public record CancelarSolicitud(@NotBlank @Size(max = 200) String motivo) {
    }

    public record RegistrarPagoSolicitud(
            @NotNull @Positive BigDecimal monto,
            LocalDate fechaPago,
            @Size(max = 100) String referencia) {
    }

    public record PagoRespuesta(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {
        static PagoRespuesta de(Pago p) {
            return new PagoRespuesta(p.getId(), p.getMonto(), p.getFechaPago(), p.getReferencia());
        }
    }

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

        static ComprobanteRespuesta de(Comprobante c) {
            return new ComprobanteRespuesta(c.getId(), c.getOrdenId(), c.getFolio(), c.getMoneda(),
                    c.getSubtotal(), c.getIva(), c.getTotal(), c.getTasaIva(), c.getEstatus(),
                    c.getMotivoCancelacion(), c.estatusCobro(), c.pagado(), c.saldo(),
                    c.getPagos().stream().map(PagoRespuesta::de).toList());
        }
    }
}
