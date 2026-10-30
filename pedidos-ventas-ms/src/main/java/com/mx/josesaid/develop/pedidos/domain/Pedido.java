package com.mx.josesaid.develop.pedidos.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Pedido: BORRADOR → CONFIRMADO → ENTREGADO. CANCELADO se alcanza desde BORRADOR o CONFIRMADO.
 * Una devolución lleva ENTREGADO a CANCELADO con motivo DEVOLUCION_TOTAL. Nada se borra.
 */
public class Pedido {

    private final UUID id;
    private final UUID clienteId;
    private final UUID cotizacionId;
    private final Canal canal;
    private final String moneda;
    private final TasaIva tasa;
    private EstatusPedido estatus;
    private List<Linea> lineas;
    private Instant fechaConfirmacion;
    private Instant fechaEntrega;
    private MotivoCancelacion motivoCancelacion;

    private Pedido(UUID id, UUID clienteId, UUID cotizacionId, Canal canal, String moneda, TasaIva tasa,
                   EstatusPedido estatus, List<Linea> lineas, Instant fechaConfirmacion, Instant fechaEntrega,
                   MotivoCancelacion motivoCancelacion) {
        this.id = id;
        this.clienteId = clienteId;
        this.cotizacionId = cotizacionId;
        this.canal = canal;
        this.moneda = moneda;
        this.tasa = tasa;
        this.estatus = estatus;
        this.lineas = new ArrayList<>(lineas);
        this.fechaConfirmacion = fechaConfirmacion;
        this.fechaEntrega = fechaEntrega;
        this.motivoCancelacion = motivoCancelacion;
    }

    public static Pedido crear(UUID clienteId, Canal canal, String moneda, TasaIva tasa, List<Linea> lineas) {
        Cotizacion.validarCabecera(clienteId, canal, moneda, tasa);
        if (lineas == null || lineas.isEmpty()) {
            throw new ReglaDeNegocioException("El pedido requiere al menos una línea");
        }
        return new Pedido(UUID.randomUUID(), clienteId, null, canal, moneda, tasa, EstatusPedido.BORRADOR, lineas,
                null, null, null);
    }

    /** Crea el pedido en borrador a partir de una cotización aceptada. */
    public static Pedido desdeCotizacion(Cotizacion c) {
        if (c.getEstatus() != EstatusCotizacion.ACEPTADA || c.getPedidoId() != null) {
            throw new ConflictoException("Solo una cotización aceptada y no convertida puede pasar a pedido");
        }
        return new Pedido(UUID.randomUUID(), c.getClienteId(), c.getId(), c.getCanal(), c.getMoneda(), c.getTasa(),
                EstatusPedido.BORRADOR, c.getLineas(), null, null, null);
    }

    public static Pedido reconstruir(UUID id, UUID clienteId, UUID cotizacionId, Canal canal, String moneda,
                                     TasaIva tasa, EstatusPedido estatus, List<Linea> lineas, Instant fechaConfirmacion,
                                     Instant fechaEntrega, MotivoCancelacion motivoCancelacion) {
        return new Pedido(id, clienteId, cotizacionId, canal, moneda, tasa, estatus, lineas, fechaConfirmacion,
                fechaEntrega, motivoCancelacion);
    }

    public void reemplazarLineas(List<Linea> nuevas) {
        if (estatus != EstatusPedido.BORRADOR) {
            throw new ConflictoException("Solo un pedido en borrador admite cambios en sus líneas");
        }
        if (nuevas == null) {
            throw new ReglaDeNegocioException("Las líneas son obligatorias");
        }
        this.lineas = new ArrayList<>(nuevas);
    }

    public void confirmar(Instant ahora) {
        if (estatus != EstatusPedido.BORRADOR) {
            throw new ConflictoException("Solo un pedido en borrador puede confirmarse");
        }
        if (lineas.isEmpty() || lineas.stream().noneMatch(l -> l.getCantidad() > 0)) {
            throw new ReglaDeNegocioException("Confirmar requiere al menos una línea con cantidad mayor a cero");
        }
        this.estatus = EstatusPedido.CONFIRMADO;
        this.fechaConfirmacion = ahora;
    }

    public void entregar(Instant ahora) {
        if (estatus != EstatusPedido.CONFIRMADO) {
            throw new ConflictoException("Solo un pedido confirmado puede entregarse");
        }
        this.estatus = EstatusPedido.ENTREGADO;
        this.fechaEntrega = ahora;
    }

    public void cancelar(MotivoCancelacion motivo) {
        if (motivo == null) {
            throw new SolicitudInvalidaException("El motivo de cancelación es obligatorio");
        }
        if (motivo == MotivoCancelacion.DEVOLUCION_TOTAL) {
            throw new SolicitudInvalidaException("La devolución se registra con su propio endpoint");
        }
        if (estatus != EstatusPedido.BORRADOR && estatus != EstatusPedido.CONFIRMADO) {
            throw new ConflictoException("Solo un pedido en borrador o confirmado puede cancelarse");
        }
        this.estatus = EstatusPedido.CANCELADO;
        this.motivoCancelacion = motivo;
    }

    /** Devolución total: solo desde ENTREGADO. */
    public void devolver() {
        if (estatus != EstatusPedido.ENTREGADO) {
            throw new ConflictoException("Solo un pedido entregado puede devolverse");
        }
        this.estatus = EstatusPedido.CANCELADO;
        this.motivoCancelacion = MotivoCancelacion.DEVOLUCION_TOTAL;
    }

    public Totales totales() {
        return Totales.calcular(lineas, moneda, tasa);
    }

    public UUID getId() { return id; }
    public UUID getClienteId() { return clienteId; }
    public UUID getCotizacionId() { return cotizacionId; }
    public Canal getCanal() { return canal; }
    public String getMoneda() { return moneda; }
    public TasaIva getTasa() { return tasa; }
    public EstatusPedido getEstatus() { return estatus; }
    public List<Linea> getLineas() { return Collections.unmodifiableList(lineas); }
    public Instant getFechaConfirmacion() { return fechaConfirmacion; }
    public Instant getFechaEntrega() { return fechaEntrega; }
    public MotivoCancelacion getMotivoCancelacion() { return motivoCancelacion; }
}
