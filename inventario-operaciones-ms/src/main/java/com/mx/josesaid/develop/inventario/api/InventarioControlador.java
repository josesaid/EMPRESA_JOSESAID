package com.mx.josesaid.develop.inventario.api;

import com.mx.josesaid.develop.inventario.application.InventarioService;
import com.mx.josesaid.develop.inventario.domain.Almacen;
import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.EstatusOrden;
import com.mx.josesaid.develop.inventario.domain.Existencia;
import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.OrdenOperacion;
import com.mx.josesaid.develop.inventario.domain.TipoMovimiento;
import com.mx.josesaid.develop.inventario.domain.TipoOrden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mx.josesaid.develop.inventario.infrastructure.idempotencia.Idempotencia;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;

/**
 * Endpoints de inventario. Los de la especificación más dos altas necesarias para usar el servicio:
 * POST /articulos (OPERACIONES) y POST /almacenes (ALMACEN).
 */
@RestController
@RequestMapping("/v1")
public class InventarioControlador {

    private final InventarioService servicio;
    private final Idempotencia idempotencia;

    /** Recibe el servicio de inventario y el control de idempotencia. */
    public InventarioControlador(InventarioService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    /** Da de alta un artículo con su SKU, unidad, mínimo y si requiere lote. */
    @PostMapping("/articulos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERACIONES')")
    public ArticuloRespuesta crearArticulo(@Valid @RequestBody CrearArticuloSolicitud s) {
        return ArticuloRespuesta.de(servicio.crearArticulo(s.sku(), s.nombre(), s.unidad(),
                Boolean.TRUE.equals(s.loteObligatorio()), s.minimo()));
    }

    /** Da de alta un almacén. */
    @PostMapping("/almacenes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALMACEN')")
    public AlmacenRespuesta crearAlmacen(@Valid @RequestBody CrearAlmacenSolicitud s) {
        return AlmacenRespuesta.de(servicio.crearAlmacen(s.nombre(), s.ubicacion()));
    }

    /** Registra un movimiento de inventario: entrada, salida, ajuste o reserva. Acepta Idempotency-Key para reintentar sin duplicar el movimiento. */
    @PostMapping("/movimientos")
    @PreAuthorize("hasRole('ALMACEN')")
    public ResponseEntity<?> registrarMovimiento(@RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
                                                 @Valid @RequestBody RegistrarMovimientoSolicitud s) {
        return idempotencia.ejecutar(clave, "POST /v1/movimientos", s, HttpStatus.CREATED,
                () -> MovimientoRespuesta.de(servicio.registrarMovimiento(s.articuloId(), s.almacenId(), s.tipo(),
                        s.cantidad(), s.lote(), s.referencia())));
    }

    /** Hasta 15 artículos por debajo de su mínimo, del mayor faltante al menor. */
    @GetMapping("/articulos/bajo-minimo")
    @PreAuthorize("hasAnyRole('ALMACEN','COMPRAS')")
    public List<ArticuloBajoMinimoRespuesta> articulosBajoMinimo() {
        return servicio.listarBajoMinimo(15).stream().map(ArticuloBajoMinimoRespuesta::de).toList();
    }

    /** Últimos 15 movimientos de un artículo, del más reciente al más antiguo. */
    @GetMapping("/movimientos")
    @PreAuthorize("hasAnyRole('ALMACEN','COMPRAS')")
    public List<MovimientoRespuesta> movimientosDeArticulo(@RequestParam UUID articulo) {
        return servicio.movimientosDeArticulo(articulo).stream().map(MovimientoRespuesta::de).toList();
    }

    /** Devuelve las existencias de un artículo en cada almacén, con lo reservado y lo disponible. */
    @GetMapping("/existencias")
    @PreAuthorize("hasAnyRole('ALMACEN','COMPRAS','VENTAS')")
    public List<ExistenciaRespuesta> existencias(@RequestParam UUID articulo) {
        return servicio.existenciasDe(articulo).stream().map(ExistenciaRespuesta::de).toList();
    }

    /** Crea una orden de producción o de servicio. Acepta Idempotency-Key. */
    @PostMapping("/ordenes-operacion")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERACIONES')")
    public OrdenRespuesta crearOrden(@Valid @RequestBody CrearOrdenSolicitud s) {
        return OrdenRespuesta.de(servicio.crearOrden(s.tipo(), s.cantidadObjetivo()));
    }

    /** Cierra una orden con la cantidad real producida o completada. */
    @PostMapping("/ordenes-operacion/{id}/cierre")
    @PreAuthorize("hasRole('OPERACIONES')")
    public OrdenRespuesta cerrarOrden(@PathVariable UUID id, @Valid @RequestBody CerrarOrdenSolicitud s) {
        return OrdenRespuesta.de(servicio.cerrarOrden(id, s.cantidadReal()));
    }

    /** Devuelve los movimientos de un lote, para trazabilidad. */
    @GetMapping("/lotes/{lote}/trazabilidad")
    @PreAuthorize("hasAnyRole('OPERACIONES','CALIDAD')")
    public List<MovimientoRespuesta> trazabilidad(@PathVariable String lote) {
        return servicio.trazabilidad(lote).stream().map(MovimientoRespuesta::de).toList();
    }

    /** Datos para dar de alta un artículo. */
    public record CrearArticuloSolicitud(
            @NotBlank @Size(max = 40) String sku,
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 10) String unidad,
            Boolean loteObligatorio,
            @DecimalMin("0") BigDecimal minimo) {
    }

    /** Datos para dar de alta un almacén. */
    public record CrearAlmacenSolicitud(@NotBlank @Size(max = 100) String nombre,
                                        @NotBlank @Size(max = 200) String ubicacion) {
    }

    /** Datos de un movimiento: artículo, almacén, tipo, cantidad, lote y referencia. */
    public record RegistrarMovimientoSolicitud(
            @NotNull UUID articuloId,
            @NotNull UUID almacenId,
            @NotNull TipoMovimiento tipo,
            @NotNull @Digits(integer = 9, fraction = 3) BigDecimal cantidad,
            @Size(max = 60) String lote,
            @Size(max = 100) String referencia) {
    }

    /** Datos para crear una orden: tipo y cantidad objetivo. */
    public record CrearOrdenSolicitud(@NotNull TipoOrden tipo, @NotNull @Digits(integer = 9, fraction = 3) BigDecimal cantidadObjetivo) {
    }

    /** Datos para cerrar una orden: cantidad real. */
    public record CerrarOrdenSolicitud(@NotNull @Digits(integer = 9, fraction = 3) BigDecimal cantidadReal) {
    }

    /** Respuesta de un artículo. */
    public record ArticuloRespuesta(UUID id, String sku, String nombre, String unidad, boolean loteObligatorio,
                                    BigDecimal minimo) {
        /** Convierte un artículo en su respuesta. */
        static ArticuloRespuesta de(Articulo a) {
            return new ArticuloRespuesta(a.getId(), a.getSku(), a.getNombre(), a.getUnidad(),
                    a.isLoteObligatorio(), a.getMinimo());
        }
    }

    /** Respuesta de un almacén. */
    public record AlmacenRespuesta(UUID id, String nombre, String ubicacion) {
        /** Convierte un almacén en su respuesta. */
        static AlmacenRespuesta de(Almacen a) {
            return new AlmacenRespuesta(a.getId(), a.getNombre(), a.getUbicacion());
        }
    }

    /** Respuesta de un movimiento. */
    public record MovimientoRespuesta(UUID id, UUID articuloId, UUID almacenId, TipoMovimiento tipo,
                                      BigDecimal cantidad, String lote, String referencia, Instant creadoUtc) {
        /** Convierte un movimiento en su respuesta. */
        static MovimientoRespuesta de(MovimientoInventario m) {
            return new MovimientoRespuesta(m.getId(), m.getArticuloId(), m.getAlmacenId(), m.getTipo(),
                    m.getCantidad(), m.getLote(), m.getReferencia(), m.getCreadoUtc());
        }
    }

    /** Respuesta de un artículo que está por debajo de su mínimo. */
    public record ArticuloBajoMinimoRespuesta(UUID articuloId, String sku, String nombre, String unidad,
                                              BigDecimal minimo, BigDecimal existencia, BigDecimal faltante) {
        /** Convierte el resultado de la consulta en su respuesta. */
        static ArticuloBajoMinimoRespuesta de(com.mx.josesaid.develop.inventario.domain.ArticuloBajoMinimo r) {
            return new ArticuloBajoMinimoRespuesta(r.articuloId(), r.sku(), r.nombre(), r.unidad(), r.minimo(),
                    r.existencia(), r.faltante());
        }
    }

    /** Respuesta de la existencia de un artículo en un almacén. */
    public record ExistenciaRespuesta(UUID almacenId, BigDecimal cantidad, BigDecimal reservado, BigDecimal disponible) {
        /** Convierte una existencia en su respuesta, con el disponible calculado. */
        static ExistenciaRespuesta de(Existencia e) {
            return new ExistenciaRespuesta(e.getAlmacenId(), e.getCantidad(), e.getReservado(), e.disponible());
        }
    }

    /** Respuesta de una orden de operación. */
    public record OrdenRespuesta(UUID id, TipoOrden tipo, EstatusOrden estatus, BigDecimal cantidadObjetivo,
                                 BigDecimal cantidadReal) {
        /** Convierte una orden en su respuesta. */
        static OrdenRespuesta de(OrdenOperacion o) {
            return new OrdenRespuesta(o.getId(), o.getTipo(), o.getEstatus(), o.getCantidadObjetivo(),
                    o.getCantidadReal());
        }
    }
}
