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

/**
 * Endpoints de inventario. Los de la especificación más dos altas necesarias para usar el servicio:
 * POST /articulos (OPERACIONES) y POST /almacenes (ALMACEN).
 */
@RestController
@RequestMapping("/v1")
public class InventarioControlador {

    private final InventarioService servicio;

    public InventarioControlador(InventarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/articulos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERACIONES')")
    public ArticuloRespuesta crearArticulo(@Valid @RequestBody CrearArticuloSolicitud s) {
        return ArticuloRespuesta.de(servicio.crearArticulo(s.sku(), s.nombre(), s.unidad(),
                Boolean.TRUE.equals(s.loteObligatorio()), s.minimo()));
    }

    @PostMapping("/almacenes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALMACEN')")
    public AlmacenRespuesta crearAlmacen(@Valid @RequestBody CrearAlmacenSolicitud s) {
        return AlmacenRespuesta.de(servicio.crearAlmacen(s.nombre(), s.ubicacion()));
    }

    @PostMapping("/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALMACEN')")
    public MovimientoRespuesta registrarMovimiento(@Valid @RequestBody RegistrarMovimientoSolicitud s) {
        return MovimientoRespuesta.de(servicio.registrarMovimiento(s.articuloId(), s.almacenId(), s.tipo(),
                s.cantidad(), s.lote(), s.referencia()));
    }

    @GetMapping("/existencias")
    @PreAuthorize("hasAnyRole('ALMACEN','COMPRAS','VENTAS')")
    public List<ExistenciaRespuesta> existencias(@RequestParam UUID articulo) {
        return servicio.existenciasDe(articulo).stream().map(ExistenciaRespuesta::de).toList();
    }

    @PostMapping("/ordenes-operacion")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OPERACIONES')")
    public OrdenRespuesta crearOrden(@Valid @RequestBody CrearOrdenSolicitud s) {
        return OrdenRespuesta.de(servicio.crearOrden(s.tipo(), s.cantidadObjetivo()));
    }

    @PostMapping("/ordenes-operacion/{id}/cierre")
    @PreAuthorize("hasRole('OPERACIONES')")
    public OrdenRespuesta cerrarOrden(@PathVariable UUID id, @Valid @RequestBody CerrarOrdenSolicitud s) {
        return OrdenRespuesta.de(servicio.cerrarOrden(id, s.cantidadReal()));
    }

    @GetMapping("/lotes/{lote}/trazabilidad")
    @PreAuthorize("hasAnyRole('OPERACIONES','CALIDAD')")
    public List<MovimientoRespuesta> trazabilidad(@PathVariable String lote) {
        return servicio.trazabilidad(lote).stream().map(MovimientoRespuesta::de).toList();
    }

    public record CrearArticuloSolicitud(
            @NotBlank @Size(max = 40) String sku,
            @NotBlank @Size(max = 150) String nombre,
            @NotBlank @Size(max = 10) String unidad,
            Boolean loteObligatorio,
            @DecimalMin("0") BigDecimal minimo) {
    }

    public record CrearAlmacenSolicitud(@NotBlank @Size(max = 100) String nombre,
                                        @NotBlank @Size(max = 200) String ubicacion) {
    }

    public record RegistrarMovimientoSolicitud(
            @NotNull UUID articuloId,
            @NotNull UUID almacenId,
            @NotNull TipoMovimiento tipo,
            @NotNull BigDecimal cantidad,
            @Size(max = 60) String lote,
            @Size(max = 100) String referencia) {
    }

    public record CrearOrdenSolicitud(@NotNull TipoOrden tipo, @NotNull BigDecimal cantidadObjetivo) {
    }

    public record CerrarOrdenSolicitud(@NotNull BigDecimal cantidadReal) {
    }

    public record ArticuloRespuesta(UUID id, String sku, String nombre, String unidad, boolean loteObligatorio,
                                    BigDecimal minimo) {
        static ArticuloRespuesta de(Articulo a) {
            return new ArticuloRespuesta(a.getId(), a.getSku(), a.getNombre(), a.getUnidad(),
                    a.isLoteObligatorio(), a.getMinimo());
        }
    }

    public record AlmacenRespuesta(UUID id, String nombre, String ubicacion) {
        static AlmacenRespuesta de(Almacen a) {
            return new AlmacenRespuesta(a.getId(), a.getNombre(), a.getUbicacion());
        }
    }

    public record MovimientoRespuesta(UUID id, UUID articuloId, UUID almacenId, TipoMovimiento tipo,
                                      BigDecimal cantidad, String lote, String referencia, Instant creadoUtc) {
        static MovimientoRespuesta de(MovimientoInventario m) {
            return new MovimientoRespuesta(m.getId(), m.getArticuloId(), m.getAlmacenId(), m.getTipo(),
                    m.getCantidad(), m.getLote(), m.getReferencia(), m.getCreadoUtc());
        }
    }

    public record ExistenciaRespuesta(UUID almacenId, BigDecimal cantidad, BigDecimal reservado, BigDecimal disponible) {
        static ExistenciaRespuesta de(Existencia e) {
            return new ExistenciaRespuesta(e.getAlmacenId(), e.getCantidad(), e.getReservado(), e.disponible());
        }
    }

    public record OrdenRespuesta(UUID id, TipoOrden tipo, EstatusOrden estatus, BigDecimal cantidadObjetivo,
                                 BigDecimal cantidadReal) {
        static OrdenRespuesta de(OrdenOperacion o) {
            return new OrdenRespuesta(o.getId(), o.getTipo(), o.getEstatus(), o.getCantidadObjetivo(),
                    o.getCantidadReal());
        }
    }
}
