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

    /** Recibe el servicio de cuentas por pagar y el control de idempotencia. */
    public CuentasPorPagarControlador(CuentasPorPagarService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    /** Últimos 15 pagos a proveedores, del más reciente al más antiguo. */
    @GetMapping("/pagos")
    @PreAuthorize("hasRole('FINANZAS')")
    public List<PagoListadoRespuesta> listarPagos() {
        return servicio.listarPagosRecientes().stream()
                .map(p -> new PagoListadoRespuesta(p.id(), p.cuentaId(), p.monto(), p.fechaPago(), p.referencia()))
                .toList();
    }

    /** Devuelve una cuenta por pagar con sus pagos. */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('FINANZAS')")
    public CuentaRespuesta obtener(@PathVariable UUID id) {
        return CuentaRespuesta.de(servicio.obtener(id));
    }

    /** Registra un pago a proveedor contra el saldo de la cuenta. Acepta Idempotency-Key. */
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

    /**
     * Datos de un pago a proveedor: monto, fecha opcional y referencia.
     *
     * @param monto componente de tipo {@code BigDecimal}.
     * @param fechaPago componente de tipo {@code LocalDate}.
     * @param referencia componente de tipo {@code String}.
     */
    public record RegistrarPagoSolicitud(
            @NotNull @Positive BigDecimal monto,
            LocalDate fechaPago,
            @Size(max = 100) String referencia) {
    }

    /**
     * Respuesta de una cuenta por pagar.
     *
     * @param id componente de tipo {@code UUID}.
     * @param ordenId componente de tipo {@code UUID}.
     * @param proveedorId componente de tipo {@code UUID}.
     * @param moneda componente de tipo {@code String}.
     * @param monto componente de tipo {@code BigDecimal}.
     * @param saldo componente de tipo {@code BigDecimal}.
     * @param estatus componente de tipo {@code String}.
     * @param pagos componente de tipo {@code List<PagoRespuesta>}.
     */
    public record CuentaRespuesta(UUID id, UUID ordenId, UUID proveedorId, String moneda, BigDecimal monto,
                                  BigDecimal saldo, String estatus, List<PagoRespuesta> pagos) {

        /** Convierte una cuenta en su respuesta. */
        static CuentaRespuesta de(CuentaPorPagar c) {
            return new CuentaRespuesta(c.getId(), c.getOrdenId(), c.getProveedorId(), c.getMoneda(), c.getMonto(),
                    c.saldo(), c.estatus().name(), c.getPagos().stream().map(PagoRespuesta::de).toList());
        }
    }

    /**
     * Pago dentro de un listado, con la cuenta a la que pertenece.
     *
     * @param id componente de tipo {@code UUID}.
     * @param cuentaId componente de tipo {@code UUID}.
     * @param monto componente de tipo {@code BigDecimal}.
     * @param fechaPago componente de tipo {@code LocalDate}.
     * @param referencia componente de tipo {@code String}.
     */
    public record PagoListadoRespuesta(UUID id, UUID cuentaId, BigDecimal monto, LocalDate fechaPago,
                                       String referencia) {
    }

    /**
     * Respuesta de un pago a proveedor.
     *
     * @param id componente de tipo {@code UUID}.
     * @param monto componente de tipo {@code BigDecimal}.
     * @param fechaPago componente de tipo {@code LocalDate}.
     * @param referencia componente de tipo {@code String}.
     */
    public record PagoRespuesta(UUID id, BigDecimal monto, LocalDate fechaPago, String referencia) {

        /** Convierte un pago a proveedor en su respuesta. */
        static PagoRespuesta de(PagoProveedor p) {
            return new PagoRespuesta(p.getId(), p.getMonto(), p.getFechaPago(), p.getReferencia());
        }
    }
}
