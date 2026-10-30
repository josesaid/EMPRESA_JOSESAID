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

    public PedidosService(CotizacionRepositorio cotizaciones, PedidoRepositorio pedidos,
                          PublicadorEventos eventos, Clock reloj,
                          ValidadorCliente clientes) {
        this.cotizaciones = cotizaciones;
        this.pedidos = pedidos;
        this.eventos = eventos;
        this.reloj = reloj;
        this.clientes = clientes;
    }

    /** Línea tal como llega de la API, antes de validarse en el dominio. */
    public record LineaComando(UUID productoId, String descripcion, int cantidad, BigDecimal precioUnitario) {
    }

    /** Ventas netas de un mes: pedidos confirmados o entregados, sin los cancelados ni devueltos. */
    public record ResumenVentas(YearMonth mes, long pedidos, Map<String, BigDecimal> ventasPorMoneda) {
    }

    @Transactional
    public Cotizacion crearCotizacion(UUID clienteId, Canal canal, String moneda, TasaIva tasa,
                                      LocalDate vigenteHasta, List<LineaComando> lineas) {
        clientes.exigirActivo(clienteId);
        return cotizaciones.guardar(Cotizacion.crear(clienteId, canal, moneda, tasa, vigenteHasta, aLineas(lineas)));
    }

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

    @Transactional
    public Pedido convertirEnPedido(UUID cotizacionId) {
        Cotizacion c = cargarCotizacion(cotizacionId);
        Pedido pedido = Pedido.desdeCotizacion(c);
        c.marcarConvertida(pedido.getId());
        cotizaciones.guardar(c);
        return pedidos.guardar(pedido);
    }

    @Transactional
    public Pedido crearPedido(UUID clienteId, Canal canal, String moneda, TasaIva tasa, List<LineaComando> lineas) {
        clientes.exigirActivo(clienteId);
        return pedidos.guardar(Pedido.crear(clienteId, canal, moneda, tasa, aLineas(lineas)));
    }

    @Transactional
    public Pedido reemplazarLineas(UUID id, List<LineaComando> lineas) {
        Pedido p = cargarPedido(id);
        p.reemplazarLineas(aLineas(lineas));
        return pedidos.guardar(p);
    }

    @Transactional
    public Pedido confirmarPedido(UUID id) {
        Pedido p = cargarPedido(id);
        p.confirmar(Instant.now(reloj));
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoConfirmado", id.toString(), Map.of(
                "clienteId", p.getClienteId().toString(),
                "moneda", p.getMoneda(),
                "total", p.totales().total())));
        return guardado;
    }

    @Transactional
    public Pedido entregarPedido(UUID id) {
        Pedido p = cargarPedido(id);
        p.entregar(Instant.now(reloj));
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoEntregado", id.toString(), Map.of(
                "clienteId", p.getClienteId().toString())));
        return guardado;
    }

    @Transactional
    public Pedido cancelarPedido(UUID id, MotivoCancelacion motivo) {
        Pedido p = cargarPedido(id);
        p.cancelar(motivo);
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoCancelado", id.toString(), Map.of("motivo", motivo.name())));
        return guardado;
    }

    @Transactional
    public Pedido devolverPedido(UUID id) {
        Pedido p = cargarPedido(id);
        p.devolver();
        Pedido guardado = pedidos.guardar(p);
        eventos.publicar(new EventoDominio("PedidoCancelado", id.toString(),
                Map.of("motivo", MotivoCancelacion.DEVOLUCION_TOTAL.name())));
        return guardado;
    }

    @Transactional(readOnly = true)
    public Cotizacion obtenerCotizacion(UUID id) {
        return cargarCotizacion(id);
    }

    @Transactional(readOnly = true)
    public Pedido obtenerPedido(UUID id) {
        return cargarPedido(id);
    }

    @Transactional(readOnly = true)
    public List<Pedido> buscarPedidos(UUID clienteId, EstatusPedido estatus, Canal canal) {
        return pedidos.buscarFiltrados(clienteId, estatus, canal);
    }

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

    private Cotizacion cargarCotizacion(UUID id) {
        return cotizaciones.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Cotización no encontrada: " + id));
    }

    private Pedido cargarPedido(UUID id) {
        return pedidos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
    }

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
