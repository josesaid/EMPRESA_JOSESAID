package com.mx.josesaid.develop.pedidos.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/** Cotización: BORRADOR → ENVIADA → ACEPTADA, o VENCIDA si pasa su fecha. Se convierte en pedido una sola vez. */
public class Cotizacion {

    private final UUID id;
    private final UUID clienteId;
    private final Canal canal;
    private final String moneda;
    private final TasaIva tasa;
    private final LocalDate vigenteHasta;
    private final List<Linea> lineas;
    private EstatusCotizacion estatus;
    private UUID pedidoId;

    /** Constructor privado: las cotizaciones se crean con sus fábricas. */
    private Cotizacion(UUID id, UUID clienteId, Canal canal, String moneda, TasaIva tasa, LocalDate vigenteHasta,
                       List<Linea> lineas, EstatusCotizacion estatus, UUID pedidoId) {
        this.id = id;
        this.clienteId = clienteId;
        this.canal = canal;
        this.moneda = moneda;
        this.tasa = tasa;
        this.vigenteHasta = vigenteHasta;
        this.lineas = new ArrayList<>(lineas);
        this.estatus = estatus;
        this.pedidoId = pedidoId;
    }

    /** Crea una cotización en borrador. Requiere vigencia y al menos una línea. */
    public static Cotizacion crear(UUID clienteId, Canal canal, String moneda, TasaIva tasa, LocalDate vigenteHasta,
                                   List<Linea> lineas) {
        validarCabecera(clienteId, canal, moneda, tasa);
        if (vigenteHasta == null) {
            throw new ReglaDeNegocioException("La vigencia de la cotización es obligatoria");
        }
        if (lineas == null || lineas.isEmpty()) {
            throw new ReglaDeNegocioException("La cotización requiere al menos una línea");
        }
        return new Cotizacion(UUID.randomUUID(), clienteId, canal, moneda, tasa, vigenteHasta, lineas,
                EstatusCotizacion.BORRADOR, null);
    }

    /** Reconstruye una cotización desde la base de datos sin aplicar reglas. */
    public static Cotizacion reconstruir(UUID id, UUID clienteId, Canal canal, String moneda, TasaIva tasa,
                                         LocalDate vigenteHasta, List<Linea> lineas, EstatusCotizacion estatus,
                                         UUID pedidoId) {
        return new Cotizacion(id, clienteId, canal, moneda, tasa, vigenteHasta, lineas, estatus, pedidoId);
    }

    /** Valida cliente, canal, moneda y tasa de IVA de la cotización. */
    static void validarCabecera(UUID clienteId, Canal canal, String moneda, TasaIva tasa) {
        if (clienteId == null) {
            throw new ReglaDeNegocioException("El cliente es obligatorio");
        }
        if (canal == null) {
            throw new ReglaDeNegocioException("El canal es obligatorio");
        }
        Monedas.decimales(moneda);
        if (tasa == null) {
            throw new ReglaDeNegocioException("La tasa de IVA es obligatoria");
        }
    }

    /** Envía la cotización: de borrador a enviada. */
    public void enviar() {
        if (estatus != EstatusCotizacion.BORRADOR) {
            throw new ConflictoException("Solo una cotización en borrador puede enviarse");
        }
        this.estatus = EstatusCotizacion.ENVIADA;
    }

    /** Acepta la cotización. Si ya venció, la marca VENCIDA y responde 410: no se reactiva. */
    public void aceptar(LocalDate hoy) {
        if (estatus == EstatusCotizacion.VENCIDA) {
            throw new CotizacionVencidaException("La cotización venció; cree una nueva");
        }
        if (estatus != EstatusCotizacion.ENVIADA) {
            throw new ConflictoException("Solo una cotización enviada puede aceptarse");
        }
        if (hoy.isAfter(vigenteHasta)) {
            this.estatus = EstatusCotizacion.VENCIDA;
            throw new CotizacionVencidaException("La cotización venció el " + vigenteHasta);
        }
        this.estatus = EstatusCotizacion.ACEPTADA;
    }

    /** Verifica que pueda convertirse y registra el pedido resultante. */
    public void marcarConvertida(UUID pedidoId) {
        if (estatus != EstatusCotizacion.ACEPTADA) {
            throw new ConflictoException("Solo una cotización aceptada puede convertirse en pedido");
        }
        if (this.pedidoId != null) {
            throw new ConflictoException("La cotización ya se convirtió en un pedido");
        }
        this.pedidoId = pedidoId;
    }

    /** Calcula subtotal, IVA y total de la cotización. */
    public Totales totales() {
        return Totales.calcular(lineas, moneda, tasa);
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el id del cliente. */
    public UUID getClienteId() { return clienteId; }
    /** Devuelve el canal. */
    public Canal getCanal() { return canal; }
    /** Devuelve la moneda. */
    public String getMoneda() { return moneda; }
    /** Devuelve la tasa de IVA. */
    public TasaIva getTasa() { return tasa; }
    /** Devuelve la fecha hasta la que es vigente. */
    public LocalDate getVigenteHasta() { return vigenteHasta; }
    /** Devuelve las líneas, en una vista de solo lectura. */
    public List<Linea> getLineas() { return Collections.unmodifiableList(lineas); }
    /** Devuelve el estatus. */
    public EstatusCotizacion getEstatus() { return estatus; }
    /** Devuelve el id del pedido creado a partir de esta cotización, o nulo. */
    public UUID getPedidoId() { return pedidoId; }
}
