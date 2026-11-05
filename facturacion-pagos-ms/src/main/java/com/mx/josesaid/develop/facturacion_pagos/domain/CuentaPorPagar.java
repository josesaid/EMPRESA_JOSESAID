package com.mx.josesaid.develop.facturacion_pagos.domain;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Cuenta por pagar a un proveedor: se crea al aprobarse una orden de compra y recibe pagos hasta saldarla.
 * Un pago no puede exceder el saldo.
 */
public class CuentaPorPagar {

    private static final Set<String> MONEDAS = Set.of("MXN", "USD");

    private final UUID id;
    private final UUID ordenId;
    private final UUID proveedorId;
    private final String moneda;
    private final BigDecimal monto;
    private final List<PagoProveedor> pagos;

    private CuentaPorPagar(UUID id, UUID ordenId, UUID proveedorId, String moneda, BigDecimal monto,
                           List<PagoProveedor> pagos) {
        this.id = id;
        this.ordenId = ordenId;
        this.proveedorId = proveedorId;
        this.moneda = moneda;
        this.monto = monto;
        this.pagos = new ArrayList<>(pagos);
    }

    public static CuentaPorPagar crear(UUID ordenId, UUID proveedorId, String moneda, BigDecimal monto) {
        if (ordenId == null) {
            throw new ReglaDeNegocioException("La cuenta por pagar necesita la orden");
        }
        if (proveedorId == null) {
            throw new ReglaDeNegocioException("La cuenta por pagar necesita el proveedor");
        }
        if (moneda == null || !MONEDAS.contains(moneda)) {
            throw new ReglaDeNegocioException("La moneda no está permitida: " + moneda);
        }
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto de la cuenta debe ser mayor a cero");
        }
        return new CuentaPorPagar(UUID.randomUUID(), ordenId, proveedorId, moneda,
                monto.setScale(2, RoundingMode.HALF_EVEN), List.of());
    }

    public static CuentaPorPagar reconstruir(UUID id, UUID ordenId, UUID proveedorId, String moneda,
                                             BigDecimal monto, List<PagoProveedor> pagos) {
        return new CuentaPorPagar(id, ordenId, proveedorId, moneda, monto, pagos);
    }

    /** Registra un pago contra el saldo pendiente. Un monto que redondea a cero o excede el saldo se rechaza. */
    public PagoProveedor registrarPago(BigDecimal montoPago, LocalDate fechaPago, String referencia) {
        if (montoPago == null) {
            throw new ReglaDeNegocioException("El monto del pago debe ser mayor a cero");
        }
        BigDecimal redondeado = montoPago.setScale(2, RoundingMode.HALF_EVEN);
        if (redondeado.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto del pago debe ser mayor a cero");
        }
        if (fechaPago == null) {
            throw new ReglaDeNegocioException("La fecha del pago es obligatoria");
        }
        if (redondeado.compareTo(saldo()) > 0) {
            throw new ReglaDeNegocioException("El pago excede el saldo pendiente");
        }
        PagoProveedor pago = new PagoProveedor(UUID.randomUUID(), redondeado, fechaPago, referencia);
        pagos.add(pago);
        return pago;
    }

    public BigDecimal pagado() {
        return pagos.stream().map(PagoProveedor::getMonto).reduce(ZERO, BigDecimal::add);
    }

    public BigDecimal saldo() {
        BigDecimal saldo = monto.subtract(pagado());
        return saldo.signum() < 0 ? ZERO : saldo;
    }

    public EstatusCuentaPorPagar estatus() {
        if (pagos.isEmpty()) {
            return EstatusCuentaPorPagar.PENDIENTE;
        }
        return saldo().signum() == 0 ? EstatusCuentaPorPagar.PAGADA : EstatusCuentaPorPagar.PARCIAL;
    }

    public UUID getId() { return id; }
    public UUID getOrdenId() { return ordenId; }
    public UUID getProveedorId() { return proveedorId; }
    public String getMoneda() { return moneda; }
    public BigDecimal getMonto() { return monto; }

    /** Vista de solo lectura de los pagos. */
    public List<PagoProveedor> getPagos() {
        return Collections.unmodifiableList(pagos);
    }
}
