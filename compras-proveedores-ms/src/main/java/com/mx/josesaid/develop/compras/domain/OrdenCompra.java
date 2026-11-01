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
    private final String creadaPor;

    /** Órdenes de 1,000,000 USD o más solo las aprueba DIRECCION. */
    public static final BigDecimal LIMITE_APROBACION_USD = new BigDecimal("1000000");

    private OrdenCompra(UUID id, UUID proveedorId, String moneda, EstatusOrdenCompra estatus,
                        String motivoCancelacion, List<LineaOrden> lineas, String creadaPor) {
        this.id = id;
        this.proveedorId = proveedorId;
        this.moneda = moneda;
        this.estatus = estatus;
        this.motivoCancelacion = motivoCancelacion;
        this.lineas = new ArrayList<>(lineas);
        this.creadaPor = creadaPor;
    }

    /** Crea la orden en BORRADOR. Un proveedor inactivo no recibe órdenes nuevas (422). */
    public static OrdenCompra crear(Proveedor proveedor, String moneda, List<LineaOrden> lineas, String creadaPor) {
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
        if (creadaPor == null || creadaPor.isBlank()) {
            throw new ReglaDeNegocioException("La orden necesita el usuario que la crea");
        }
        return new OrdenCompra(UUID.randomUUID(), proveedor.getId(), moneda, EstatusOrdenCompra.BORRADOR, null, lineas, creadaPor);
    }

    public static OrdenCompra reconstruir(UUID id, UUID proveedorId, String moneda, EstatusOrdenCompra estatus,
                                          String motivoCancelacion, List<LineaOrden> lineas, String creadaPor) {
        return new OrdenCompra(id, proveedorId, moneda, estatus, motivoCancelacion, lineas, creadaPor);
    }

    /**
     * Aprueba la orden si el usuario puede hacerlo:
     * nadie aprueba una orden que creó; las de 1,000,000 USD o más solo las aprueba DIRECCION;
     * las órdenes antiguas, sin creador registrado, solo las aprueba DIRECCION.
     */
    public void aprobar(String usuario, boolean esDireccion, BigDecimal tipoCambioMxnPorUsd) {
        if (estatus != EstatusOrdenCompra.BORRADOR) {
            throw new ConflictoException("Solo una orden en borrador puede aprobarse");
        }
        if (creadaPor == null) {
            if (!esDireccion) {
                throw new AprobacionNoPermitidaException(
                        "Esta orden se creó antes de registrar su creador; solo DIRECCION puede aprobarla");
            }
        } else if (creadaPor.equals(usuario)) {
            throw new AprobacionNoPermitidaException("No puedes aprobar una orden que tú creaste");
        } else if (!esDireccion && totalEnUsd(tipoCambioMxnPorUsd).compareTo(LIMITE_APROBACION_USD) >= 0) {
            throw new AprobacionNoPermitidaException(
                    "Las órdenes de 1,000,000 USD o más solo puede aprobarlas DIRECCION");
        }
        this.estatus = EstatusOrdenCompra.APROBADA;
    }

    /** Total en USD para comparar contra el límite. Las órdenes en MXN se convierten con el tipo de cambio. */
    public BigDecimal totalEnUsd(BigDecimal tipoCambioMxnPorUsd) {
        if ("USD".equals(moneda)) {
            return total();
        }
        return total().divide(tipoCambioMxnPorUsd, 2, RoundingMode.HALF_UP);
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

    public String getCreadaPor() { return creadaPor; }
    public UUID getId() { return id; }
    public UUID getProveedorId() { return proveedorId; }
    public String getMoneda() { return moneda; }
    public EstatusOrdenCompra getEstatus() { return estatus; }
    public String getMotivoCancelacion() { return motivoCancelacion; }
    public List<LineaOrden> getLineas() { return Collections.unmodifiableList(lineas); }
}
