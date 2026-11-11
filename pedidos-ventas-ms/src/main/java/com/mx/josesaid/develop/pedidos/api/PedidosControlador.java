package com.mx.josesaid.develop.pedidos.api;

import com.mx.josesaid.develop.pedidos.application.PedidosService;
import com.mx.josesaid.develop.pedidos.application.PedidosService.LineaComando;
import com.mx.josesaid.develop.pedidos.domain.Canal;
import com.mx.josesaid.develop.pedidos.domain.Cotizacion;
import com.mx.josesaid.develop.pedidos.domain.EstatusCotizacion;
import com.mx.josesaid.develop.pedidos.domain.EstatusPedido;
import com.mx.josesaid.develop.pedidos.domain.Linea;
import com.mx.josesaid.develop.pedidos.domain.MotivoCancelacion;
import com.mx.josesaid.develop.pedidos.domain.Monedas;
import com.mx.josesaid.develop.pedidos.domain.Pedido;
import com.mx.josesaid.develop.pedidos.domain.TasaIva;
import com.mx.josesaid.develop.pedidos.infrastructure.idempotencia.Idempotencia;
import com.mx.josesaid.develop.pedidos.domain.Totales;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * API de ventas. Autorización por rol: VENTAS opera cotizaciones y pedidos, ALMACEN entrega,
 * DIRECCION y FINANZAS consultan métricas. Los errores salen como RFC 9457 desde ManejadorErrores.
 */
@RestController
@RequestMapping("/v1")
public class PedidosControlador {

    private final PedidosService servicio;
    private final Idempotencia idempotencia;

    /** Recibe el servicio de ventas y el control de idempotencia. */
    public PedidosControlador(PedidosService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    // ---- Cotizaciones ----

    /** Crea una cotización en borrador para un cliente activo. */
    @PostMapping("/cotizaciones")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public ResponseEntity<CotizacionRespuesta> crearCotizacion(@Valid @RequestBody CrearCotizacionPeticion p) {
        Cotizacion c = servicio.crearCotizacion(p.clienteId(), p.canal(), p.moneda(), p.tasaIva(),
                p.vigenteHasta(), aComandos(p.lineas()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CotizacionRespuesta.de(c));
    }

    /** Devuelve una cotización por id. */
    @GetMapping("/cotizaciones/{id}")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION','FINANZAS')")
    public CotizacionRespuesta obtenerCotizacion(@PathVariable UUID id) {
        return CotizacionRespuesta.de(servicio.obtenerCotizacion(id));
    }

    /** Envía la cotización al cliente: pasa de borrador a enviada. */
    @PostMapping("/cotizaciones/{id}/envio")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public CotizacionRespuesta enviarCotizacion(@PathVariable UUID id) {
        return CotizacionRespuesta.de(servicio.enviarCotizacion(id));
    }

    /** Acepta la cotización. Si ya venció, responde 410 y la marca como vencida. */
    @PostMapping("/cotizaciones/{id}/aceptacion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public CotizacionRespuesta aceptarCotizacion(@PathVariable UUID id) {
        return CotizacionRespuesta.de(servicio.aceptarCotizacion(id));
    }

    /** Convierte una cotización aceptada en un pedido en borrador. Solo puede hacerse una vez. */
    @PostMapping("/cotizaciones/{id}/pedido")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public ResponseEntity<PedidoRespuesta> convertirEnPedido(@PathVariable UUID id) {
        Pedido p = servicio.convertirEnPedido(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoRespuesta.de(p));
    }

    // ---- Pedidos ----

    /** Crea un pedido en borrador directamente, sin cotización. Acepta Idempotency-Key. */
    @PostMapping("/pedidos")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public ResponseEntity<?> crearPedido(@RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
                                         @Valid @RequestBody CrearPedidoPeticion p) {
        return idempotencia.ejecutar(clave, "POST /v1/pedidos", p, HttpStatus.CREATED,
                () -> PedidoRespuesta.de(servicio.crearPedido(p.clienteId(), p.canal(), p.moneda(), p.tasaIva(), aComandos(p.lineas()))));
    }

    /** Busca pedidos por cliente, estatus o canal. Todos los filtros son opcionales. */
    @GetMapping("/pedidos")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION','FINANZAS')")
    public List<PedidoRespuesta> buscarPedidos(@RequestParam(required = false) UUID clienteId,
                                               @RequestParam(required = false) EstatusPedido estatus,
                                               @RequestParam(required = false) Canal canal) {
        return servicio.buscarPedidos(clienteId, estatus, canal).stream().map(PedidoRespuesta::de).toList();
    }

    /** Devuelve un pedido por id. */
    @GetMapping("/pedidos/{id}")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION','FINANZAS','ALMACEN')")
    public PedidoRespuesta obtenerPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.obtenerPedido(id));
    }

    /** Reemplaza las líneas de un pedido. Solo se permite mientras está en borrador. */
    @PutMapping("/pedidos/{id}/lineas")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta reemplazarLineas(@PathVariable UUID id, @Valid @RequestBody LineasPeticion p) {
        return PedidoRespuesta.de(servicio.reemplazarLineas(id, aComandos(p.lineas())));
    }

    /** Confirma el pedido y publica el evento para que inventario reserve las piezas. */
    @PostMapping("/pedidos/{id}/confirmacion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta confirmarPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.confirmarPedido(id));
    }

    /** Marca el pedido como entregado y publica el evento para que inventario descuente las piezas. */
    @PostMapping("/pedidos/{id}/entrega")
    @PreAuthorize("hasAnyRole('ALMACEN','DIRECCION')")
    public PedidoRespuesta entregarPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.entregarPedido(id));
    }

    /** Cancela un pedido en borrador o confirmado. Requiere el motivo. */
    @PostMapping("/pedidos/{id}/cancelacion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta cancelarPedido(@PathVariable UUID id, @Valid @RequestBody CancelacionPeticion p) {
        return PedidoRespuesta.de(servicio.cancelarPedido(id, p.motivo()));
    }

    /** Registra la devolución total de un pedido entregado. Lo pasa a cancelado con motivo de devolución. */
    @PostMapping("/pedidos/{id}/devolucion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta devolverPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.devolverPedido(id));
    }

    // ---- Métricas y catálogos ----

    /** Devuelve el resumen de ventas netas del mes indicado, por moneda. */
    @GetMapping("/metricas/ventas")
    @PreAuthorize("hasAnyRole('DIRECCION','FINANZAS')")
    public ResumenVentasRespuesta resumenVentas(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        PedidosService.ResumenVentas r = servicio.resumenVentas(mes);
        return new ResumenVentasRespuesta(r.mes().toString(), r.pedidos(), r.ventasPorMoneda());
    }

    /** Catálogo de tasas de IVA disponibles. */
    @GetMapping("/catalogos/tasas-iva")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> tasasIva() {
        return Arrays.stream(TasaIva.values())
                .map(t -> Map.<String, Object>of("codigo", t.name(), "tasa", t.valor()))
                .toList();
    }

    /** Catálogo de monedas con sus decimales. */
    @GetMapping("/catalogos/monedas")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Integer> monedas() {
        return Monedas.catalogo();
    }

    // ---- Mapeo de peticiones ----

    /** Convierte las líneas recibidas en la API en comandos de la capa de aplicación. */
    private static List<LineaComando> aComandos(List<LineaPeticion> lineas) {
        if (lineas == null) {
            return List.of();
        }
        return lineas.stream()
                .map(l -> new LineaComando(l.productoId(), l.descripcion(), l.cantidad(), l.precioUnitario()))
                .toList();
    }

    // ---- Peticiones ----

    /**
     * Línea de una cotización o pedido: producto, cantidad y precio.
     *
     * @param productoId identificador del producto, de tipo {@code UUID}.
     * @param descripcion descripcion, de tipo {@code String}.
     * @param cantidad cantidad solicitada o registrada, de tipo {@code int}.
     * @param precioUnitario precio unitario, de tipo {@code BigDecimal}.
     */
    public record LineaPeticion(
            @NotNull UUID productoId,
            @Size(max = 200) String descripcion,
            @Min(1) int cantidad,
            @NotNull @DecimalMin("0.00") BigDecimal precioUnitario) {
    }

    /**
     * Datos para crear una cotización.
     *
     * @param clienteId identificador del cliente, de tipo {@code UUID}.
     * @param canal canal, de tipo {@code Canal}.
     * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
     * @param tasaIva tasa de IVA aplicable, de tipo {@code TasaIva}.
     * @param vigenteHasta vigente hasta, de tipo {@code LocalDate}.
     * @param lineas líneas o renglones del documento, de tipo {@code List<LineaPeticion>}.
     */
    public record CrearCotizacionPeticion(
            @NotNull UUID clienteId,
            @NotNull Canal canal,
            @NotBlank String moneda,
            @NotNull TasaIva tasaIva,
            @NotNull LocalDate vigenteHasta,
            @NotEmpty @Size(max = 100) List<@Valid LineaPeticion> lineas) {
    }

    /**
     * Datos para crear un pedido directo.
     *
     * @param clienteId identificador del cliente, de tipo {@code UUID}.
     * @param canal canal, de tipo {@code Canal}.
     * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
     * @param tasaIva tasa de IVA aplicable, de tipo {@code TasaIva}.
     * @param lineas líneas o renglones del documento, de tipo {@code List<LineaPeticion>}.
     */
    public record CrearPedidoPeticion(
            @NotNull UUID clienteId,
            @NotNull Canal canal,
            @NotBlank String moneda,
            @NotNull TasaIva tasaIva,
            @NotEmpty @Size(max = 100) List<@Valid LineaPeticion> lineas) {
    }

    /**
     * Lista de líneas para reemplazar las del pedido.
     *
     * @param lineas líneas o renglones del documento, de tipo {@code List<LineaPeticion>}.
     */
    public record LineasPeticion(@NotNull @Size(max = 100) List<@Valid LineaPeticion> lineas) {
    }

    /**
     * Motivo de la cancelación.
     *
     * @param motivo motivo del registro o de la acción, de tipo {@code MotivoCancelacion}.
     */
    public record CancelacionPeticion(@NotNull MotivoCancelacion motivo) {
    }

    // ---- Respuestas ----

    /**
     * Respuesta de una línea.
     *
     * @param productoId identificador del producto, de tipo {@code UUID}.
     * @param descripcion descripcion, de tipo {@code String}.
     * @param cantidad cantidad solicitada o registrada, de tipo {@code int}.
     * @param precioUnitario precio unitario, de tipo {@code BigDecimal}.
     * @param subtotal subtotal antes de impuestos, de tipo {@code BigDecimal}.
     */
    public record LineaRespuesta(UUID productoId, String descripcion, int cantidad,
                                 BigDecimal precioUnitario, BigDecimal subtotal) {
        /** Convierte las líneas del dominio en su respuesta. */
        static List<LineaRespuesta> de(List<Linea> lineas) {
            return lineas.stream()
                    .map(l -> new LineaRespuesta(l.getProductoId(), l.getDescripcion(), l.getCantidad(),
                            l.getPrecioUnitario(), l.subtotal()))
                    .toList();
        }
    }

    /**
     * Respuesta de una cotización.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param clienteId identificador del cliente, de tipo {@code UUID}.
     * @param canal canal, de tipo {@code Canal}.
     * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
     * @param tasaIva tasa de IVA aplicable, de tipo {@code TasaIva}.
     * @param vigenteHasta vigente hasta, de tipo {@code LocalDate}.
     * @param estatus estado actual del registro, de tipo {@code EstatusCotizacion}.
     * @param pedidoId identificador del pedido, de tipo {@code UUID}.
     * @param lineas líneas o renglones del documento, de tipo {@code List<LineaRespuesta>}.
     * @param totales totales, de tipo {@code Totales}.
     */
    public record CotizacionRespuesta(UUID id, UUID clienteId, Canal canal, String moneda, TasaIva tasaIva,
                                      LocalDate vigenteHasta, EstatusCotizacion estatus, UUID pedidoId,
                                      List<LineaRespuesta> lineas, Totales totales) {
        /** Convierte una cotización en su respuesta. */
        static CotizacionRespuesta de(Cotizacion c) {
            return new CotizacionRespuesta(c.getId(), c.getClienteId(), c.getCanal(), c.getMoneda(), c.getTasa(),
                    c.getVigenteHasta(), c.getEstatus(), c.getPedidoId(),
                    LineaRespuesta.de(c.getLineas()), c.totales());
        }
    }

    /**
     * Respuesta de un pedido.
     *
     * @param id identificador único del registro, de tipo {@code UUID}.
     * @param clienteId identificador del cliente, de tipo {@code UUID}.
     * @param cotizacionId identificador del cotizacion, de tipo {@code UUID}.
     * @param canal canal, de tipo {@code Canal}.
     * @param moneda código de moneda (por ejemplo, MXN), de tipo {@code String}.
     * @param tasaIva tasa de IVA aplicable, de tipo {@code TasaIva}.
     * @param estatus estado actual del registro, de tipo {@code EstatusPedido}.
     * @param lineas líneas o renglones del documento, de tipo {@code List<LineaRespuesta>}.
     * @param totales totales, de tipo {@code Totales}.
     * @param fechaConfirmacion fecha confirmacion, de tipo {@code Instant}.
     * @param fechaEntrega fecha entrega, de tipo {@code Instant}.
     * @param motivoCancelacion motivo cancelacion, de tipo {@code MotivoCancelacion}.
     */
    public record PedidoRespuesta(UUID id, UUID clienteId, UUID cotizacionId, Canal canal, String moneda,
                                  TasaIva tasaIva, EstatusPedido estatus, List<LineaRespuesta> lineas,
                                  Totales totales, Instant fechaConfirmacion, Instant fechaEntrega,
                                  MotivoCancelacion motivoCancelacion) {
        /** Convierte un pedido en su respuesta. */
        static PedidoRespuesta de(Pedido p) {
            return new PedidoRespuesta(p.getId(), p.getClienteId(), p.getCotizacionId(), p.getCanal(),
                    p.getMoneda(), p.getTasa(), p.getEstatus(), LineaRespuesta.de(p.getLineas()),
                    p.totales(), p.getFechaConfirmacion(), p.getFechaEntrega(), p.getMotivoCancelacion());
        }
    }

    /**
     * Resumen de ventas de un mes.
     *
     * @param mes mes, de tipo {@code String}.
     * @param pedidos pedidos, de tipo {@code long}.
     * @param ventasPorMoneda ventas por moneda, de tipo {@code Map<String, BigDecimal>}.
     */
    public record ResumenVentasRespuesta(String mes, long pedidos, Map<String, BigDecimal> ventasPorMoneda) {
    }
}
