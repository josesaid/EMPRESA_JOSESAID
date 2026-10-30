package com.mx.josesaid.develop.facturacion_pagos.domain;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_EVEN;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado raíz de facturación.
 *
 * <p>Reglas: un comprobante emitido no se modifica; un pago no excede el saldo; solo se cancela
 * un comprobante emitido y la cancelación exige motivo. No depende de Spring, JPA ni Kafka.
 *
 * <p>El folio es provisional. El timbrado con el PAC y el folio fiscal quedan pendientes.
 */
public class Comprobante {

    /** Monedas aceptadas (ISO 4217). Agregar una moneda es configuración del área. */
    public static final Set<String> MONEDAS = Set.of("MXN", "USD");

    private final UUID id;
    private final UUID ordenId;
    private final String moneda;
    private final BigDecimal subtotal;
    private final BigDecimal iva;
    private final BigDecimal total;
    private final BigDecimal tasaIva;
    private String folio;
    private EstatusComprobante estatus;
    private String motivoCancelacion;
    private final List<Pago> pagos;

    private Comprobante(UUID id, UUID ordenId, String moneda, BigDecimal subtotal, BigDecimal iva,
                        BigDecimal total, BigDecimal tasaIva, String folio, EstatusComprobante estatus,
                        String motivoCancelacion, List<Pago> pagos) {
        this.id = id;
        this.ordenId = ordenId;
        this.moneda = moneda;
        this.subtotal = subtotal;
        this.iva = iva;
        this.total = total;
        this.tasaIva = tasaIva;
        this.folio = folio;
        this.estatus = estatus;
        this.motivoCancelacion = motivoCancelacion;
        this.pagos = new ArrayList<>(pagos);
    }

    /** Crea un comprobante en borrador. El IVA y el total se calculan con redondeo bancario. */
    public static Comprobante crearBorrador(UUID ordenId, String moneda, BigDecimal subtotal, BigDecimal tasaIva) {
        if (ordenId == null) {
            throw new ReglaDeNegocioException("La orden es obligatoria");
        }
        if (moneda == null || !MONEDAS.contains(moneda)) {
            throw new ReglaDeNegocioException("La moneda no está permitida: " + moneda);
        }
        if (subtotal == null || subtotal.signum() <= 0) {
            throw new ReglaDeNegocioException("El subtotal debe ser mayor a cero");
        }
        if (tasaIva == null || tasaIva.signum() < 0 || tasaIva.compareTo(BigDecimal.ONE) > 0) {
            throw new ReglaDeNegocioException("La tasa de IVA debe estar entre 0 y 1");
        }
        BigDecimal subtotalRedondeado = subtotal.setScale(2, HALF_EVEN);
        BigDecimal ivaCalculado = subtotalRedondeado.multiply(tasaIva).setScale(2, HALF_EVEN);
        BigDecimal totalCalculado = subtotalRedondeado.add(ivaCalculado);
        return new Comprobante(UUID.randomUUID(), ordenId, moneda, subtotalRedondeado, ivaCalculado,
                totalCalculado, tasaIva, null, EstatusComprobante.BORRADOR, null, List.of());
    }

    /** Reconstruye desde la persistencia sin volver a validar. */
    public static Comprobante reconstruir(UUID id, UUID ordenId, String moneda, BigDecimal subtotal,
                                          BigDecimal iva, BigDecimal total, BigDecimal tasaIva, String folio,
                                          EstatusComprobante estatus, String motivoCancelacion, List<Pago> pagos) {
        return new Comprobante(id, ordenId, moneda, subtotal, iva, total, tasaIva, folio, estatus,
                motivoCancelacion, pagos);
    }

    /** Emite el comprobante y asigna el folio. Solo desde BORRADOR. */
    public void emitir() {
        if (estatus != EstatusComprobante.BORRADOR) {
            throw new ReglaDeNegocioException("Solo un comprobante en borrador puede emitirse");
        }
        this.folio = generarFolioProvisional();
        this.estatus = EstatusComprobante.EMITIDO;
    }

    /** Cancela un comprobante emitido. El motivo es obligatorio. El registro no se borra. */
    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new ReglaDeNegocioException("El motivo de cancelación es obligatorio");
        }
        if (estatus != EstatusComprobante.EMITIDO) {
            throw new ReglaDeNegocioException("Solo un comprobante emitido puede cancelarse");
        }
        this.estatus = EstatusComprobante.CANCELADO;
        this.motivoCancelacion = motivo.trim();
    }

    /**
     * Registra un pago contra el saldo pendiente. Solo en comprobantes emitidos.
     * Si la fecha es nula, la capa de aplicación la asigna.
     */
    public Pago registrarPago(BigDecimal monto, LocalDate fechaPago, String referencia) {
        if (estatus != EstatusComprobante.EMITIDO) {
            throw new ReglaDeNegocioException("Solo se pueden registrar pagos en comprobantes emitidos");
        }
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto del pago debe ser mayor a cero");
        }
        if (fechaPago == null) {
            throw new ReglaDeNegocioException("La fecha del pago es obligatoria");
        }
        BigDecimal montoRedondeado = monto.setScale(2, HALF_EVEN);
        if (montoRedondeado.compareTo(saldo()) > 0) {
            throw new ReglaDeNegocioException("El pago excede el saldo pendiente");
        }
        Pago pago = new Pago(UUID.randomUUID(), montoRedondeado, fechaPago, referencia);
        pagos.add(pago);
        return pago;
    }

    public BigDecimal pagado() {
        return pagos.stream().map(Pago::getMonto).reduce(ZERO, BigDecimal::add);
    }

    public BigDecimal saldo() {
        BigDecimal saldo = total.subtract(pagado());
        return saldo.signum() < 0 ? ZERO : saldo;
    }

    public EstatusCobro estatusCobro() {
        if (pagado().signum() == 0) {
            return EstatusCobro.PENDIENTE;
        }
        return saldo().signum() == 0 ? EstatusCobro.PAGADO : EstatusCobro.PARCIAL;
    }

    /** Folio provisional de 8 caracteres. Se reemplazará por el folio fiscal del PAC. */
    private static String generarFolioProvisional() {
        return "FAC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public UUID getId() { return id; }
    public UUID getOrdenId() { return ordenId; }
    public String getMoneda() { return moneda; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getIva() { return iva; }
    public BigDecimal getTotal() { return total; }
    public BigDecimal getTasaIva() { return tasaIva; }
    public String getFolio() { return folio; }
    public EstatusComprobante getEstatus() { return estatus; }
    public String getMotivoCancelacion() { return motivoCancelacion; }

    /** Vista de solo lectura de los pagos. */
    public List<Pago> getPagos() {
        return Collections.unmodifiableList(pagos);
    }
}
