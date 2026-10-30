package com.mx.josesaid.develop.compras.domain;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Orden de compra. Una orden aprobada no se modifica: solo se recibe o se cancela.
 * La recepción no puede superar lo ordenado (409).
 */
public class OrdenCompra {

    private static final java.util.Set<String> MONEDAS = java.util.Set.of("MXN", "USD");

    private final UUID id;
    private final UUID proveedorId;
    private final String moneda;
    private EstatusOrdenCompra estatus;
    private String motivoCancelacion;
    private final List<LineaOrden> lineas;

    private OrdenCompra(UUID id, UUID proveedorId, String moneda, EstatusOrdenCompra estatus,
                        String motivoCancelacion, List<LineaOrden> lineas) {
        this.id = id;
        this.proveedorId = proveedorId;
        this.moneda = moneda;
        this.estatus = estatus;
        this.motivoCancelacion = motivoCancelacion;
        this.lineas = new ArrayList<>(lineas);
    }

    /** Crea la orden en BORRADOR. Un proveedor inactivo no recibe órdenes nuevas (422). */
    public static OrdenCompra crear(Proveedor proveedor, String moneda, List<LineaOrden> lineas) {
        if (proveedor == null) {
            throw new RecursoNoEncontradoException("Proveedor no encontrado");
        }
        if (!proveedor.estaActivo()) {
            throw new ReglaDeNegocioException("El proveedor no está activo y no recibe órdenes nuevas");
        }
        if (moneda == null || !MONEDAS.contains(moneda)) {
            throw new ReglaDeNegocioException("La moneda no está permitida: " + moneda);
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new ReglaDeNegocioException("La orden requiere al menos una línea");
        }
        return new OrdenCompra(UUID.randomUUID(), proveedor.getId(), moneda, EstatusOrdenCompra.BORRADOR, null, lineas);
    }

    public static OrdenCompra reconstruir(UUID id, UUID proveedorId, String moneda, EstatusOrdenCompra estatus,
                                          String motivoCancelacion, List<LineaOrden> lineas) {
        return new OrdenCompra(id, proveedorId, moneda, estatus, motivoCancelacion, lineas);
    }

    public void aprobar() {
        if (estatus != EstatusOrdenCompra.BORRADOR) {
            throw new ConflictoException("Solo una orden en borrador puede aprobarse");
        }
        this.estatus = EstatusOrdenCompra.APROBADA;
    }

    /** Registra una recepción de una línea. Devuelve la recepción para su historial. */
    public Recepcion recibir(String sku, BigDecimal cantidad) {
        if (estatus != EstatusOrdenCompra.APROBADA && estatus != EstatusOrdenCompra.RECIBIDA_PARCIAL) {
            throw new ConflictoException("Solo una orden aprobada admite recepciones");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad recibida debe ser mayor a cero");
        }
        int indice = indiceDe(sku);
        LineaOrden linea = lineas.get(indice);
        BigDecimal cantidadRedondeada = cantidad.setScale(3, RoundingMode.HALF_EVEN);
        BigDecimal pendiente = linea.getCantidadOrdenada().subtract(linea.getCantidadRecibida());
        if (cantidadRedondeada.compareTo(pendiente) > 0) {
            throw new ConflictoException("La recepción excede lo ordenado para " + sku
                    + ": pendiente " + pendiente.toPlainString());
        }
        lineas.set(indice, linea.conRecepcion(cantidadRedondeada));
        this.estatus = lineas.stream().allMatch(LineaOrden::completa)
                ? EstatusOrdenCompra.RECIBIDA : EstatusOrdenCompra.RECIBIDA_PARCIAL;
        return new Recepcion(UUID.randomUUID(), id, sku, cantidadRedondeada);
    }

    public void cancelar(String motivo) {
        if (motivo == null || motivo.isBlank()) {
            throw new SolicitudInvalidaException("El motivo de cancelación es obligatorio");
        }
        if (estatus != EstatusOrdenCompra.BORRADOR && estatus != EstatusOrdenCompra.APROBADA) {
            throw new ConflictoException("Solo una orden en borrador o aprobada puede cancelarse");
        }
        this.estatus = EstatusOrdenCompra.CANCELADA;
        this.motivoCancelacion = motivo.trim();
    }

    public BigDecimal total() {
        return lineas.stream().map(LineaOrden::subtotal).reduce(ZERO, BigDecimal::add);
    }

    private int indiceDe(String sku) {
        for (int i = 0; i < lineas.size(); i++) {
            if (lineas.get(i).getArticuloSku().equals(sku)) {
                return i;
            }
        }
        throw new ReglaDeNegocioException("La orden no contiene el SKU " + sku);
    }

    public UUID getId() { return id; }
    public UUID getProveedorId() { return proveedorId; }
    public String getMoneda() { return moneda; }
    public EstatusOrdenCompra getEstatus() { return estatus; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public List<LineaOrden> getLineas() { return Collections.unmodifiableList(lineas); }
}
