package com.mx.josesaid.develop.pedidos.application;

import com.mx.josesaid.develop.pedidos.domain.Canal;
import com.mx.josesaid.develop.pedidos.domain.Cotizacion;
import com.mx.josesaid.develop.pedidos.domain.CotizacionRepositorio;
import com.mx.josesaid.develop.pedidos.domain.CotizacionVencidaException;
import com.mx.josesaid.develop.pedidos.domain.EstatusPedido;
import com.mx.josesaid.develop.pedidos.domain.Linea;
import com.mx.josesaid.develop.pedidos.domain.MotivoCancelacion;
import com.mx.josesaid.develop.pedidos.domain.Monedas;
import com.mx.josesaid.develop.pedidos.domain.Pedido;
import com.mx.josesaid.develop.pedidos.domain.PedidoRepositorio;
import com.mx.josesaid.develop.pedidos.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.pedidos.domain.TasaIva;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de ventas: cotizaciones, pedidos, devoluciones y métricas. */
@Service
public class PedidosService {

    private final CotizacionRepositorio cotizaciones;
    private final PedidoRepositorio pedidos;
    private final PublicadorEventos eventos;
    private final Clock reloj;
    private final ValidadorCliente clientes;

    /** Recibe los repositorios, el publicador de eventos, el reloj y el validador de clientes. */
    public PedidosService(CotizacionRepositorio cotizaciones, PedidoRepositorio pedidos,
                          PublicadorEventos eventos, Clock reloj,
                          ValidadorCliente clientes) {
        this.cotizaciones = cotizaciones;
        this.pedidos = pedidos;
        this.eventos = eventos;
        this.reloj = reloj;
        this.clientes = clientes;
    }

    /**
     * Línea tal como llega de la API, antes de validarse en el dominio.
     *
     * @param productoId identificador del producto, de tipo {@code UUID}.
     * @param descripcion descripcion, de tipo {@code String}.
     * @param cantidad cantidad solicitada o registrada, de tipo {@code int}.
     * @param precioUnitario precio unitario, de tipo {@code BigDecimal}.
     */
    public record LineaComando(UUID productoId, String descripcion, int cantidad, BigDecimal precioUnitario) {
    }

    /**
     * Ventas netas de un mes: pedidos confirmados o entregados, sin los cancelados ni devueltos.
     *
     * @param mes mes, de tipo {@code YearMonth}.
     * @param pedidos pedidos, de tipo {@code long}.
     * @param ventasPorMoneda ventas por moneda, de tipo {@code Map<String, BigDecimal>}.
     */
    public record ResumenVentas(YearMonth mes, long pedidos, Map<String, BigDecimal> ventasPorMoneda) {
    }

    /** Crea una cotización en borrador. Falla si el cliente no está activo. */
    @Transactional
    public Cotizacion crearCotizacion(UUID clienteId, Canal canal, String moneda, TasaIva tasa,
                                      LocalDate vigenteHasta, List<LineaComando> lineas) {
        clientes.exigirActivo(clienteId);
        return cotizaciones.guardar(Cotizacion.crear(clienteId, canal, moneda, tasa, vigenteHasta, aLineas(lineas)));
    }

    /** Envía la cotización al cliente. */
    @Transactional
    public Cotizacion enviarCotizacion(UUID id) {
        Cotizacion c = cargarCotizacion(id);
        c.enviar();
        return cotizaciones.guardar(c);
    }

    /**
     * Si la cotización venció, se guarda como VENCIDA y se responde 410. Por eso no se revierte la transacción.
     */
    @Transactional(noRollbackFor = CotizacionVencidaException.class)
    public Cotizacion aceptarCotizacion(UUID id) {
        Cotizacion c = cargarCotizacion(id);
        try {
            c.aceptar(LocalDate.now(reloj));
        } catch (CotizacionVencidaException e) {
            cotizaciones.guardar(c);
            throw e;
        }
        return cotizaciones.guardar(c);
    }

    /** Convierte una cotización aceptada en pedido en borrador. Verifica que el cliente siga activo. */
    @Transactional
    public Pedido convertirEnPedido(UUID cotizacionId) {
        Cotizacion c = cargarCotizacion(cotizacionId);
        clientes.exigirActivo(c.getClienteId());
        Pedido pedido = Pedido.desdeCotizacion(c);
        c.marcarConvertida(pedido.getId());
        cotizaciones.guardar(c);
        return pedidos.guardar(pedido);
    }

    /** Crea un pedido en borrador. Falla si el cliente no está activo. */
    @Transactional
    public Pedido crearPedido(UUID clienteId, Canal canal, String moneda, TasaIva tasa, List<LineaComando> lineas) {
        clientes.exigirActivo(clienteId);
        return pedidos.guardar(Pedido.crear(clienteId, canal, moneda, tasa, aLineas(lineas)));
    }

    /** Reemplaza las líneas de un pedido en borrador. */
    @Transactional
    public Pedido reemplazarLineas(UUID id, List<LineaComando> lineas) {
        Pedido p = cargarPedido(id);
        p.reemplazarLineas(aLineas(lineas));
        return pedidos.guardar(p);
    }

    /** Confirma el pedido y publica PedidoConfirmado con el total y las líneas, para que inventario reserve las piezas. */
    @Transactional
    public Pedido confirmarPedido(UUID id) {
        Pedido p = cargarPedido(id);
        p.confirmar(Instant.now(reloj));
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoConfirmado", id.toString(), Map.of(
                "pedidoId", id.toString(),
                "clienteId", p.getClienteId().toString(),
                "moneda", p.getMoneda(),
                "total", p.totales().total(),
                "lineas", lineasParaEvento(p))));
        return guardado;
    }

    /** Entrega el pedido y publica PedidoEntregado con las líneas, para que inventario descuente las piezas. */
    @Transactional
    public Pedido entregarPedido(UUID id) {
        Pedido p = cargarPedido(id);
        p.entregar(Instant.now(reloj));
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoEntregado", id.toString(), Map.of(
                "pedidoId", id.toString(),
                "clienteId", p.getClienteId().toString(),
                "lineas", lineasParaEvento(p))));
        return guardado;
    }

    /** Artículo y cantidad de cada línea, para que inventario reserve y descuente sin consultar a ventas. */
    private static List<Map<String, Object>> lineasParaEvento(Pedido p) {
        return p.getLineas().stream()
                .map(l -> Map.<String, Object>of(
                        "productoId", l.getProductoId().toString(),
                        "cantidad", l.getCantidad()))
                .toList();
    }

    /** Cancela el pedido y publica PedidoCancelado con el motivo. */
    @Transactional
    public Pedido cancelarPedido(UUID id, MotivoCancelacion motivo) {
        Pedido p = cargarPedido(id);
        p.cancelar(motivo);
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoCancelado", id.toString(), Map.of("motivo", motivo.name())));
        return guardado;
    }

    /** Registra la devolución total y publica PedidoCancelado con motivo de devolución. */
    @Transactional
    public Pedido devolverPedido(UUID id) {
        Pedido p = cargarPedido(id);
        p.devolver();
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoCancelado", id.toString(),
                Map.of("motivo", MotivoCancelacion.DEVOLUCION_TOTAL.name())));
        return guardado;
    }

    /** Devuelve una cotización por id. */
    @Transactional(readOnly = true)
    public Cotizacion obtenerCotizacion(UUID id) {
        return cargarCotizacion(id);
    }

    /** Devuelve un pedido por id. */
    @Transactional(readOnly = true)
    public Pedido obtenerPedido(UUID id) {
        return cargarPedido(id);
    }

    /** Busca pedidos con filtros opcionales. */
    @Transactional(readOnly = true)
    public List<Pedido> buscarPedidos(UUID clienteId, EstatusPedido estatus, Canal canal) {
        return pedidos.buscarFiltrados(clienteId, estatus, canal);
    }

    /** Calcula las ventas netas del mes en UTC: pedidos confirmados o entregados, sin los cancelados ni devueltos, por moneda. */
    @Transactional(readOnly = true)
    public ResumenVentas resumenVentas(YearMonth mes) {
        Instant desde = mes.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant hasta = mes.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        List<Pedido> lista = pedidos.buscarConfirmadosEntre(desde, hasta);
        Map<String, BigDecimal> porMoneda = lista.stream().collect(Collectors.groupingBy(
                Pedido::getMoneda, TreeMap::new,
                Collectors.reducing(BigDecimal.ZERO, p -> p.totales().total(), BigDecimal::add)));
        return new ResumenVentas(mes, lista.size(), porMoneda);
    }

    /** Carga una cotización por id o lanza RecursoNoEncontradoException. */
    private Cotizacion cargarCotizacion(UUID id) {
        return cotizaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización no encontrada: " + id));
    }

    /** Carga un pedido por id o lanza RecursoNoEncontradoException. */
    private Pedido cargarPedido(UUID id) {
        return pedidos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

    /** Convierte los comandos en líneas del dominio. */
    private static List<Linea> aLineas(List<LineaComando> comandos) {
        if (comandos == null) {
            return List.of();
        }
        return comandos.stream()
                .map(c -> Linea.crear(c.productoId(), c.descripcion(), c.cantidad(), c.precioUnitario()))
                .toList();
    }

    /** Catálogo de monedas, usado por la API para mostrar opciones válidas. */
    public static Map<String, Integer> monedas() {
        return Monedas.catalogo();
    }
}
