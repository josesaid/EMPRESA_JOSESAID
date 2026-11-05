package com.mx.josesaid.develop.facturacion_pagos.api;

import com.mx.josesaid.develop.facturacion_pagos.application.CuentasPorPagarService;
import com.mx.josesaid.develop.facturacion_pagos.domain.CuentaPorPagar;
import com.mx.josesaid.develop.facturacion_pagos.domain.PagoProveedor;
import com.mx.josesaid.develop.facturacion_pagos.infrastructure.idempotencia.Idempotencia;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pagos a proveedores. Solo FINANZAS lee y registra. */
@RestController
@RequestMapping("/v1/cuentas-por-pagar")
public class CuentasPorPagarControlador {

    private final CuentasPorPagarService servicio;
    private final Idempotencia idempotencia;

    public CuentasPorPagarControlador(CuentasPorPagarService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FINANZAS')")
    public CuentaRespuesta obtener(@PathVariable UUID id) {
        return CuentaRespuesta.de(servicio.obtener(id));
    }

    @PostMapping("/{id}/pagos")
    @PreAuthorize("hasRole('FINANZAS')")
    public ResponseEntity<?> registrarPago(@RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
                                           @PathVariable UUID id,
                                           @Valid @RequestBody RegistrarPagoSolicitud solicitud) {
        return idempotencia.ejecutar(clave, "POST /v1/cuentas-por-pagar/" + id + "/pagos", solicitud,
                HttpStatus.CREATED,
                () -> CuentaRespuesta.de(servicio.registrarPago(
                        id, solicitud.monto(), solicitud.fechaPago(), solicitud.referencia())));
    }

    public record RegistrarPagoSolicitud(
            @NotNull @Positive BigDecimal monto,
            LocalDate fechaPago,
            @Size(max = 100) String referencia) {
    }

    public record CuentaRespuesta(UUID id, UUID ordenId, UUID proveedorId, String moneda, BigDecimal monto,
                                  BigDecimal saldo, String estatus, List<PagoRespuesta> pagos) {

        static CuentaRespuesta de(CuentaPorPagar c) {
            return new CuentaRespuesta(c.getId(), c.getOrdenId(), c.getProveedorId(), c.getMoneda(), c.getMonto(),
                    c.saldo(), c.estatus().name(), c.getPagos().stream().map(PagoRespuesta::de).toList());
        }
    }

    public record PagoRespuesta(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {

        static PagoRespuesta de(PagoProveedor p) {
            return new PagoRespuesta(p.getId(), p.getMonto(), p.getFechaPago(), p.getReferencia());
        }
    }
}
