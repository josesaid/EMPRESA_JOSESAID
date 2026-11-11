package com.mx.josesaid.develop.compras.api;

import com.mx.josesaid.develop.compras.application.ComprasService;
import com.mx.josesaid.develop.compras.application.LineaRecibida;
import com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra;
import com.mx.josesaid.develop.compras.domain.EstatusProveedor;
import com.mx.josesaid.develop.compras.domain.LineaOrden;
import com.mx.josesaid.develop.compras.domain.OrdenCompra;
import com.mx.josesaid.develop.compras.domain.Proveedor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.mx.josesaid.develop.compras.infrastructure.idempotencia.Idempotencia;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;

/**
 * Proveedores y órdenes de compra. Los datos fiscales se enmascaran en las respuestas.
 * GET /ordenes-compra/{id} se añade a la especificación para consultar el estado de cada compra.
 */
@Validated
@RestController
@RequestMapping("/v1")
public class ComprasControlador {

    private final ComprasService servicio;
    private final Idempotencia idempotencia;

    /** Recibe el servicio de compras y el control de idempotencia. */
    public ComprasControlador(ComprasService servicio, Idempotencia idempotencia) {
        this.servicio = servicio;
        this.idempotencia = idempotencia;
    }

    /** Da de alta un proveedor. */
    @PostMapping("/proveedores")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COMPRAS')")
    public ProveedorRespuesta crearProveedor(@Valid @RequestBody CrearProveedorSolicitud s) {
        return ProveedorRespuesta.de(servicio.crearProveedor(s.razonSocial(), s.rfc(), s.correo()));
    }

    /** Busca proveedores por texto en la razón social. */
    @GetMapping("/proveedores")
    @PreAuthorize("hasAnyRole('COMPRAS','FINANZAS')")
    public List<ProveedorRespuesta> buscarProveedores(@RequestParam @Size(max = 250) String razonSocial) {
        return servicio.buscarProveedores(razonSocial).stream().map(ProveedorRespuesta::de).toList();
    }

    /** Devuelve un proveedor por id. */
    @GetMapping("/proveedores/{id}")
    @PreAuthorize("hasAnyRole('COMPRAS','FINANZAS')")
    public ProveedorRespuesta obtenerProveedor(@PathVariable UUID id) {
        return ProveedorRespuesta.de(servicio.obtenerProveedor(id));
    }

    /** Crea una orden de compra en borrador. Acepta Idempotency-Key. */
    @PostMapping("/ordenes-compra")
    @PreAuthorize("hasRole('COMPRAS')")
    public ResponseEntity<?> crearOrden(@RequestHeader(value = Idempotencia.CABECERA, required = false) String clave,
                                        @Valid @RequestBody CrearOrdenSolicitud s,
                                        @AuthenticationPrincipal Jwt jwt) {
        return idempotencia.ejecutar(clave, "POST /v1/ordenes-compra", s, HttpStatus.CREATED, () -> {
            List<LineaOrden> lineas = s.lineas().stream()
                    .map(l -> LineaOrden.nueva(l.articuloSku(), l.cantidad(), l.precioUnitario()))
                    .toList();
            return OrdenRespuesta.de(servicio.crearOrden(s.proveedorId(), s.moneda(), lineas, jwt.getSubject()));
        });
    }

    /** Lista las órdenes recientes, opcionalmente filtradas por estatus. */
    @GetMapping("/ordenes-compra")
    @PreAuthorize("hasAnyRole('COMPRAS','ALMACEN','DIRECCION')")
    public List<ResumenOrdenRespuesta> listarOrdenes(
            @RequestParam(required = false) com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra estatus) {
        return servicio.listarOrdenesRecientes(estatus).stream()
                .map(r -> new ResumenOrdenRespuesta(r.id(), r.estatus(), r.razonSocialProveedor(), r.creadaUtc()))
                .toList();
    }

    /** Devuelve una orden de compra con sus líneas. */
    @GetMapping("/ordenes-compra/{id}")
    @PreAuthorize("hasAnyRole('COMPRAS','ALMACEN','DIRECCION')")
    public OrdenRespuesta obtenerOrden(@PathVariable UUID id) {
        return OrdenRespuesta.de(servicio.obtenerOrden(id));
    }

    /** Aprueba la orden si el usuario puede hacerlo. Las reglas de aprobación están en la orden. */
    @PostMapping("/ordenes-compra/{id}/aprobacion")
    @PreAuthorize("hasAnyRole('COMPRAS','DIRECCION')")
    public OrdenRespuesta aprobarOrden(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt,
                                       Authentication autenticacion) {
        boolean esDireccion = autenticacion.getAuthorities().stream()
                .anyMatch(a -> "ROLE_DIRECCION".equals(a.getAuthority()));
        return OrdenRespuesta.de(servicio.aprobarOrden(id, jwt.getSubject(), esDireccion));
    }

    /** Registra la recepción de mercancía. No puede superar lo ordenado. */
    @PostMapping("/ordenes-compra/{id}/recepciones")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ALMACEN')")
    public OrdenRespuesta registrarRecepcion(@PathVariable UUID id, @Valid @RequestBody RecepcionSolicitud s) {
        return OrdenRespuesta.de(servicio.registrarRecepcion(id,
                s.lineas().stream().map(l -> new LineaRecibida(l.articuloSku(), l.cantidad())).toList()));
    }

    /** Cancela una orden en borrador o aprobada. Requiere motivo. */
    @PostMapping("/ordenes-compra/{id}/cancelacion")
    @PreAuthorize("hasRole('COMPRAS')")
    public OrdenRespuesta cancelarOrden(@PathVariable UUID id, @Valid @RequestBody CancelarSolicitud s) {
        return OrdenRespuesta.de(servicio.cancelarOrden(id, s.motivo()));
    }

    /**
     * Datos para dar de alta un proveedor.
     *
     * @param razonSocial componente de tipo {@code String}.
     * @param rfc componente de tipo {@code String}.
     * @param correo componente de tipo {@code String}.
     */
    public record CrearProveedorSolicitud(
            @NotBlank @Size(max = 200) String razonSocial,
            @NotBlank @Size(max = 13) String rfc,
            @NotBlank @Email @Size(max = 254) String correo) {
    }

    /**
     * Línea de una orden: SKU, cantidad y precio unitario.
     *
     * @param articuloSku componente de tipo {@code String}.
     * @param cantidad componente de tipo {@code BigDecimal}.
     * @param precioUnitario componente de tipo {@code BigDecimal}.
     */
    public record LineaSolicitud(
            @NotBlank @Size(max = 40) String articuloSku,
            @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal cantidad,
            @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal precioUnitario) {
    }

    /**
     * Datos para crear una orden: proveedor, moneda y líneas.
     *
     * @param proveedorId componente de tipo {@code UUID}.
     * @param moneda componente de tipo {@code String}.
     * @param lineas componente de tipo {@code List<LineaSolicitud>}.
     */
    public record CrearOrdenSolicitud(
            @NotNull UUID proveedorId,
            @NotBlank @Size(max = 3) String moneda,
            @NotEmpty @Size(max = 100) List<@Valid LineaSolicitud> lineas) {
    }

    /**
     * Línea recibida: SKU y cantidad.
     *
     * @param articuloSku componente de tipo {@code String}.
     * @param cantidad componente de tipo {@code BigDecimal}.
     */
    public record LineaRecepcionSolicitud(@NotBlank String articuloSku, @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal cantidad) {
    }

    /**
     * Lista de líneas recibidas en una recepción.
     *
     * @param lineas componente de tipo {@code List<LineaRecepcionSolicitud>}.
     */
    public record RecepcionSolicitud(@NotEmpty @Size(max = 100) List<@Valid LineaRecepcionSolicitud> lineas) {
    }

    /**
     * Motivo de la cancelación, de hasta 200 caracteres.
     *
     * @param motivo componente de tipo {@code String}.
     */
    public record CancelarSolicitud(@NotBlank @Size(max = 200) String motivo) {
    }

    /**
     * Respuesta de un proveedor.
     *
     * @param id componente de tipo {@code UUID}.
     * @param razonSocial componente de tipo {@code String}.
     * @param rfc componente de tipo {@code String}.
     * @param correo componente de tipo {@code String}.
     * @param estatus componente de tipo {@code EstatusProveedor}.
     */
    public record ProveedorRespuesta(UUID id, String razonSocial, String rfc, String correo, EstatusProveedor estatus) {
        /** Convierte un proveedor en su respuesta. */
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

    /**
     * Respuesta de una línea de orden, con las cantidades ordenada y recibida.
     *
     * @param articuloSku componente de tipo {@code String}.
     * @param cantidadOrdenada componente de tipo {@code BigDecimal}.
     * @param cantidadRecibida componente de tipo {@code BigDecimal}.
     * @param precioUnitario componente de tipo {@code BigDecimal}.
     * @param subtotal componente de tipo {@code BigDecimal}.
     */
    public record LineaRespuesta(String articuloSku, BigDecimal cantidadOrdenada, BigDecimal cantidadRecibida,
                                 BigDecimal precioUnitario, BigDecimal subtotal) {
    }

    /**
     * Resumen de una orden para los listados.
     *
     * @param id componente de tipo {@code UUID}.
     * @param estatus componente de tipo {@code EstatusOrdenCompra}.
     * @param razonSocialProveedor componente de tipo {@code String}.
     * @param creadaUtc componente de tipo {@code java.time.Instant}.
     */
    public record ResumenOrdenRespuesta(UUID id, EstatusOrdenCompra estatus, String razonSocialProveedor,
                                        java.time.Instant creadaUtc) {
    }

    /**
     * Respuesta de una orden de compra.
     *
     * @param id componente de tipo {@code UUID}.
     * @param proveedorId componente de tipo {@code UUID}.
     * @param estatus componente de tipo {@code EstatusOrdenCompra}.
     * @param moneda componente de tipo {@code String}.
     * @param total componente de tipo {@code BigDecimal}.
     * @param motivoCancelacion componente de tipo {@code String}.
     * @param lineas componente de tipo {@code List<LineaRespuesta>}.
     */
    public record OrdenRespuesta(UUID id, UUID proveedorId, EstatusOrdenCompra estatus, String moneda,
                                 BigDecimal total, String motivoCancelacion, List<LineaRespuesta> lineas) {
        /** Convierte una orden en su respuesta. */
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
