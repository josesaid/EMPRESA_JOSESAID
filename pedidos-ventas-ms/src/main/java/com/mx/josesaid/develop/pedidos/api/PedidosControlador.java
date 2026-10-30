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

    public PedidosControlador(PedidosService servicio) {
        this.servicio = servicio;
    }

    // ---- Cotizaciones ----

    @PostMapping("/cotizaciones")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public ResponseEntity<CotizacionRespuesta> crearCotizacion(@Valid @RequestBody CrearCotizacionPeticion p) {
        Cotizacion c = servicio.crearCotizacion(p.clienteId(), p.canal(), p.moneda(), p.tasaIva(),
                p.vigenteHasta(), aComandos(p.lineas()));
        return ResponseEntity.status(HttpStatus.CREATED).body(CotizacionRespuesta.de(c));
    }

    @GetMapping("/cotizaciones/{id}")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION','FINANZAS')")
    public CotizacionRespuesta obtenerCotizacion(@PathVariable UUID id) {
        return CotizacionRespuesta.de(servicio.obtenerCotizacion(id));
    }

    @PostMapping("/cotizaciones/{id}/envio")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public CotizacionRespuesta enviarCotizacion(@PathVariable UUID id) {
        return CotizacionRespuesta.de(servicio.enviarCotizacion(id));
    }

    @PostMapping("/cotizaciones/{id}/aceptacion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public CotizacionRespuesta aceptarCotizacion(@PathVariable UUID id) {
        return CotizacionRespuesta.de(servicio.aceptarCotizacion(id));
    }

    @PostMapping("/cotizaciones/{id}/pedido")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public ResponseEntity<PedidoRespuesta> convertirEnPedido(@PathVariable UUID id) {
        Pedido p = servicio.convertirEnPedido(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoRespuesta.de(p));
    }

    // ---- Pedidos ----

    @PostMapping("/pedidos")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public ResponseEntity<PedidoRespuesta> crearPedido(@Valid @RequestBody CrearPedidoPeticion p) {
        Pedido pedido = servicio.crearPedido(p.clienteId(), p.canal(), p.moneda(), p.tasaIva(), aComandos(p.lineas()));
        return ResponseEntity.status(HttpStatus.CREATED).body(PedidoRespuesta.de(pedido));
    }

    @GetMapping("/pedidos")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION','FINANZAS')")
    public List<PedidoRespuesta> buscarPedidos(@RequestParam(required = false) UUID clienteId,
                                               @RequestParam(required = false) EstatusPedido estatus,
                                               @RequestParam(required = false) Canal canal) {
        return servicio.buscarPedidos(clienteId, estatus, canal).stream().map(PedidoRespuesta::de).toList();
    }

    @GetMapping("/pedidos/{id}")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION','FINANZAS','ALMACEN')")
    public PedidoRespuesta obtenerPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.obtenerPedido(id));
    }

    @PutMapping("/pedidos/{id}/lineas")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta reemplazarLineas(@PathVariable UUID id, @Valid @RequestBody LineasPeticion p) {
        return PedidoRespuesta.de(servicio.reemplazarLineas(id, aComandos(p.lineas())));
    }

    @PostMapping("/pedidos/{id}/confirmacion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta confirmarPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.confirmarPedido(id));
    }

    @PostMapping("/pedidos/{id}/entrega")
    @PreAuthorize("hasAnyRole('ALMACEN','DIRECCION')")
    public PedidoRespuesta entregarPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.entregarPedido(id));
    }

    @PostMapping("/pedidos/{id}/cancelacion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta cancelarPedido(@PathVariable UUID id, @Valid @RequestBody CancelacionPeticion p) {
        return PedidoRespuesta.de(servicio.cancelarPedido(id, p.motivo()));
    }

    @PostMapping("/pedidos/{id}/devolucion")
    @PreAuthorize("hasAnyRole('VENTAS','DIRECCION')")
    public PedidoRespuesta devolverPedido(@PathVariable UUID id) {
        return PedidoRespuesta.de(servicio.devolverPedido(id));
    }

    // ---- Métricas y catálogos ----

    @GetMapping("/metricas/ventas")
    @PreAuthorize("hasAnyRole('DIRECCION','FINANZAS')")
    public ResumenVentasRespuesta resumenVentas(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes) {
        PedidosService.ResumenVentas r = servicio.resumenVentas(mes);
        return new ResumenVentasRespuesta(r.mes().toString(), r.pedidos(), r.ventasPorMoneda());
    }

    @GetMapping("/catalogos/tasas-iva")
    @PreAuthorize("isAuthenticated()")
    public List<Map<String, Object>> tasasIva() {
        return Arrays.stream(TasaIva.values())
                .map(t -> Map.<String, Object>of("codigo", t.name(), "tasa", t.valor()))
                .toList();
    }

    @GetMapping("/catalogos/monedas")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Integer> monedas() {
        return Monedas.catalogo();
    }

    // ---- Mapeo de peticiones ----

    private static List<LineaComando> aComandos(List<LineaPeticion> lineas) {
        if (lineas == null) {
            return List.of();
        }
        return lineas.stream()
                .map(l -> new LineaComando(l.productoId(), l.descripcion(), l.cantidad(), l.precioUnitario()))
                .toList();
    }

    // ---- Peticiones ----

    public record LineaPeticion(
            @NotNull UUID productoId,
            @Size(max = 200) String descripcion,
            @Min(1) int cantidad,
            @NotNull @DecimalMin("0.00") BigDecimal precioUnitario) {
    }

    public record CrearCotizacionPeticion(
            @NotNull UUID clienteId,
            @NotNull Canal canal,
            @NotBlank String moneda,
            @NotNull TasaIva tasaIva,
            @NotNull LocalDate vigenteHasta,
            @NotEmpty List<@Valid LineaPeticion> lineas) {
    }

    public record CrearPedidoPeticion(
            @NotNull UUID clienteId,
            @NotNull Canal canal,
            @NotBlank String moneda,
            @NotNull TasaIva tasaIva,
            @NotEmpty List<@Valid LineaPeticion> lineas) {
    }

    public record LineasPeticion(@NotNull List<@Valid LineaPeticion> lineas) {
    }

    public record CancelacionPeticion(@NotNull MotivoCancelacion motivo) {
    }

    // ---- Respuestas ----

    public record LineaRespuesta(UUID productoId, String descripcion, int cantidad,
                                 BigDecimal precioUnitario, BigDecimal subtotal) {
        static List<LineaRespuesta> de(List<Linea> lineas) {
            return lineas.stream()
                    .map(l -> new LineaRespuesta(l.getProductoId(), l.getDescripcion(), l.getCantidad(),
                            l.getPrecioUnitario(), l.subtotal()))
                    .toList();
        }
    }

    public record CotizacionRespuesta(UUID id, UUID clienteId, Canal canal, String moneda, TasaIva tasaIva,
                                      LocalDate vigenteHasta, EstatusCotizacion estatus, UUID pedidoId,
                                      List<LineaRespuesta> lineas, Totales totales) {
        static CotizacionRespuesta de(Cotizacion c) {
            return new CotizacionRespuesta(c.getId(), c.getClienteId(), c.getCanal(), c.getMoneda(), c.getTasa(),
                    c.getVigenteHasta(), c.getEstatus(), c.getPedidoId(),
                    LineaRespuesta.de(c.getLineas()), c.totales());
        }
    }

    public record PedidoRespuesta(UUID id, UUID clienteId, UUID cotizacionId, Canal canal, String moneda,
                                  TasaIva tasaIva, EstatusPedido estatus, List<LineaRespuesta> lineas,
                                  Totales totales, Instant fechaConfirmacion, Instant fechaEntrega,
                                  MotivoCancelacion motivoCancelacion) {
        static PedidoRespuesta de(Pedido p) {
            return new PedidoRespuesta(p.getId(), p.getClienteId(), p.getCotizacionId(), p.getCanal(),
                    p.getMoneda(), p.getTasa(), p.getEstatus(), LineaRespuesta.de(p.getLineas()),
                    p.totales(), p.getFechaConfirmacion(), p.getFechaEntrega(), p.getMotivoCancelacion());
        }
    }

    public record ResumenVentasRespuesta(String mes, long pedidos, Map<String, BigDecimal> ventasPorMoneda) {
    }
}
