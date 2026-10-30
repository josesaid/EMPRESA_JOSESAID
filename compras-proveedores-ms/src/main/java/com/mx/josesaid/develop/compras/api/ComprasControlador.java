package com.mx.josesaid.develop.compras.api;

import com.mx.josesaid.develop.compras.application.ComprasService;
import com.mx.josesaid.develop.compras.application.LineaRecibida;
import com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra;
import com.mx.josesaid.develop.compras.domain.EstatusProveedor;
import com.mx.josesaid.develop.compras.domain.LineaOrden;
import com.mx.josesaid.develop.compras.domain.OrdenCompra;
import com.mx.josesaid.develop.compras.domain.Proveedor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Proveedores y órdenes de compra. Los datos fiscales se enmascaran en las respuestas.
 * GET /ordenes-compra/{id} se añade a la especificación para consultar el estado de cada compra.
 */
@RestController
@RequestMapping("/v1")
public class ComprasControlador {

    private final ComprasService servicio;

    public ComprasControlador(ComprasService servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPRAS')")
    public ProveedorRespuesta crearProveedor(@Valid @RequestBody CrearProveedorSolicitud s) {
        return ProveedorRespuesta.de(servicio.crearProveedor(s.razonSocial(), s.rfc(), s.correo()));
    }

    @GetMapping("/proveedores/{id}")
    @PreAuthorize("hasAnyRole('COMPRAS','FINANZAS')")
    public ProveedorRespuesta obtenerProveedor(@PathVariable UUID id) {
        return ProveedorRespuesta.de(servicio.obtenerProveedor(id));
    }

    @PostMapping("/ordenes-compra")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPRAS')")
    public OrdenRespuesta crearOrden(@Valid @RequestBody CrearOrdenSolicitud s) {
        List<LineaOrden> lineas = s.lineas().stream()
                .map(l -> LineaOrden.nueva(l.articuloSku(), l.cantidad(), l.precioUnitario()))
                .toList();
        return OrdenRespuesta.de(servicio.crearOrden(s.proveedorId(), s.moneda(), lineas));
    }

    @GetMapping("/ordenes-compra/{id}")
    @PreAuthorize("hasAnyRole('COMPRAS','ALMACEN','DIRECCION')")
    public OrdenRespuesta obtenerOrden(@PathVariable UUID id) {
        return OrdenRespuesta.de(servicio.obtenerOrden(id));
    }

    @PostMapping("/ordenes-compra/{id}/aprobacion")
    @PreAuthorize("hasRole('DIRECCION')")
    public OrdenRespuesta aprobarOrden(@PathVariable UUID id) {
        return OrdenRespuesta.de(servicio.aprobarOrden(id));
    }

    @PostMapping("/ordenes-compra/{id}/recepciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALMACEN')")
    public OrdenRespuesta registrarRecepcion(@PathVariable UUID id, @Valid @RequestBody RecepcionSolicitud s) {
        return OrdenRespuesta.de(servicio.registrarRecepcion(id,
                s.lineas().stream().map(l -> new LineaRecibida(l.articuloSku(), l.cantidad())).toList()));
    }

    @PostMapping("/ordenes-compra/{id}/cancelacion")
    @PreAuthorize("hasRole('COMPRAS')")
    public OrdenRespuesta cancelarOrden(@PathVariable UUID id, @Valid @RequestBody CancelarSolicitud s) {
        return OrdenRespuesta.de(servicio.cancelarOrden(id, s.motivo()));
    }

    public record CrearProveedorSolicitud(
            @NotBlank @Size(max = 200) String razonSocial,
            @NotBlank @Size(max = 13) String rfc,
            @NotBlank @Email @Size(max = 254) String correo) {
    }

    public record LineaSolicitud(
            @NotBlank @Size(max = 40) String articuloSku,
            @NotNull @Positive BigDecimal cantidad,
            @NotNull @DecimalMin("0") BigDecimal precioUnitario) {
    }

    public record CrearOrdenSolicitud(
            @NotNull UUID proveedorId,
            @NotBlank @Size(max = 3) String moneda,
            @NotEmpty List<@Valid LineaSolicitud> lineas) {
    }

    public record LineaRecepcionSolicitud(@NotBlank String articuloSku, @NotNull @Positive BigDecimal cantidad) {
    }

    public record RecepcionSolicitud(@NotEmpty List<@Valid LineaRecepcionSolicitud> lineas) {
    }

    public record CancelarSolicitud(@NotBlank @Size(max = 200) String motivo) {
    }

    public record ProveedorRespuesta(UUID id, String razonSocial, String rfc, String correo, EstatusProveedor estatus) {
        static ProveedorRespuesta de(Proveedor p) {
            return new ProveedorRespuesta(p.getId(), p.getRazonSocial(), enmascararRfc(p.getRfc()),
                    enmascararCorreo(p.getCorreo()), p.getEstatus());
        }

        /** Muestra las primeras 3 y últimas 2 posiciones del RFC. */
        static String enmascararRfc(String rfc) {
            if (rfc == null || rfc.length() < 6) {
                return "***";
            }
            return rfc.substring(0, 3) + "*".repeat(rfc.length() - 5) + rfc.substring(rfc.length() - 2);
        }

        /** Muestra la primera letra del correo y el dominio. */
        static String enmascararCorreo(String correo) {
            int arroba = correo.indexOf('@');
            return arroba <= 0 ? "***" : correo.charAt(0) + "***" + correo.substring(arroba);
        }
    }

    public record LineaRespuesta(String articuloSku, BigDecimal cantidadOrdenada, BigDecimal cantidadRecibida,
                                 BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    public record OrdenRespuesta(UUID id, UUID proveedorId, EstatusOrdenCompra estatus, String moneda,
                                 BigDecimal total, String motivoCancelacion, List<LineaRespuesta> lineas) {
        static OrdenRespuesta de(OrdenCompra o) {
            return new OrdenRespuesta(o.getId(), o.getProveedorId(), o.getEstatus(), o.getMoneda(), o.total(),
                    o.getMotivoCancelacion(),
                    o.getLineas().stream()
                            .map(l -> new LineaRespuesta(l.getArticuloSku(), l.getCantidadOrdenada(),
                                    l.getCantidadRecibida(), l.getPrecioUnitario(), l.subtotal()))
                            .toList());
        }
    }
}
