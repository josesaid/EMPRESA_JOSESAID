package com.mx.josesaid.develop.compras.application;

import com.mx.josesaid.develop.compras.domain.ConflictoException;
import com.mx.josesaid.develop.compras.domain.EstatusOrdenCompra;
import com.mx.josesaid.develop.compras.domain.LineaOrden;
import com.mx.josesaid.develop.compras.domain.OrdenCompra;
import com.mx.josesaid.develop.compras.domain.OrdenCompraRepositorio;
import com.mx.josesaid.develop.compras.domain.Proveedor;
import com.mx.josesaid.develop.compras.domain.ProveedorRepositorio;
import com.mx.josesaid.develop.compras.domain.Recepcion;
import com.mx.josesaid.develop.compras.domain.RecepcionRepositorio;
import com.mx.josesaid.develop.compras.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.compras.domain.ResumenOrdenCompra;
import com.mx.josesaid.develop.compras.domain.SolicitudInvalidaException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de compras. Cada cambio de estado relevante se publica por outbox en la misma transacción. */
@Service
public class ComprasService {

    public static final String EVENTO_ORDEN_APROBADA = "orden-compra-aprobada";
    public static final String EVENTO_INSUMOS_RECIBIDOS = "insumos-recibidos";
    /** Límite fijo del listado de órdenes para las herramientas de IA y la API. */
    public static final int LIMITE_LISTADO_ORDENES = 15;

    /**
     * Fila del listado: sin RFC, sin montos y sin líneas.
     *
     * @param id componente de tipo {@code UUID}.
     * @param estatus componente de tipo {@code EstatusOrdenCompra}.
     * @param razonSocialProveedor componente de tipo {@code String}.
     * @param creadaUtc componente de tipo {@code Instant}.
     */
    public record ResumenListado(UUID id, EstatusOrdenCompra estatus, String razonSocialProveedor, Instant creadaUtc) {
    }

    private final ProveedorRepositorio proveedores;
    private final OrdenCompraRepositorio ordenes;
    private final RecepcionRepositorio recepciones;
    private final PublicadorEventos eventos;
    private final BigDecimal tipoCambioMxnPorUsd;
    private final BigDecimal limiteAprobacionUsd;

    /** Recibe los repositorios de proveedores, órdenes y recepciones, el publicador de eventos, el reloj y los parámetros de aprobación por monto. */
    public ComprasService(ProveedorRepositorio proveedores, OrdenCompraRepositorio ordenes,
                          RecepcionRepositorio recepciones, PublicadorEventos eventos,
                          @Value("${compras.tipo-cambio-mxn-por-usd:18}") BigDecimal tipoCambioMxnPorUsd,
                          @Value("${compras.limite-aprobacion-usd:1000000}") BigDecimal limiteAprobacionUsd) {
        if (limiteAprobacionUsd.signum() <= 0 || limiteAprobacionUsd.stripTrailingZeros().scale() > 0) {
            throw new IllegalArgumentException("compras.limite-aprobacion-usd debe ser un monto entero mayor a cero en USD");
        }
        this.proveedores = proveedores;
        this.ordenes = ordenes;
        this.recepciones = recepciones;
        this.eventos = eventos;
        this.tipoCambioMxnPorUsd = tipoCambioMxnPorUsd;
        this.limiteAprobacionUsd = limiteAprobacionUsd;
    }

    /** Da de alta un proveedor. Rechaza el RFC si ya está registrado. */
    @Transactional
    public Proveedor crearProveedor(String razonSocial, String rfc, String correo) {
        Proveedor proveedor = Proveedor.crear(razonSocial, rfc, correo);
        if (proveedores.existePorRfc(proveedor.getRfc())) {
            throw new ConflictoException("Ya existe un proveedor con ese RFC");
        }
        return proveedores.guardar(proveedor);
    }

    /** Busca proveedores por texto en la razón social. */
    @Transactional(readOnly = true)
    public List<Proveedor> buscarProveedores(String texto) {
        String termino = texto == null ? "" : texto.trim();
        if (termino.length() < 3) {
            throw new SolicitudInvalidaException("La búsqueda requiere al menos 3 caracteres");
        }
        return proveedores.buscarPorRazonSocial(termino);
    }

    /** Devuelve un proveedor por id o lanza RecursoNoEncontradoException. */
    public Proveedor obtenerProveedor(UUID id) {
        return proveedores.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
    }

    /** Crea una orden de compra en borrador y guarda quién la creó. */
    @Transactional
    public OrdenCompra crearOrden(UUID proveedorId, String moneda, List<LineaOrden> lineas, String creadaPor) {
        Proveedor proveedor = obtenerProveedor(proveedorId);
        return ordenes.guardar(OrdenCompra.crear(proveedor, moneda, lineas, creadaPor));
    }

    /** Lista las órdenes recientes, opcionalmente filtradas por estatus. */
    @Transactional(readOnly = true)
    public List<ResumenListado> listarOrdenesRecientes(EstatusOrdenCompra estatus) {
        return ordenes.listarRecientes(estatus, LIMITE_LISTADO_ORDENES).stream()
                .map(r -> new ResumenListado(r.id(), r.estatus(),
                        proveedores.buscarPorId(r.proveedorId()).map(Proveedor::getRazonSocial).orElse(""),
                        r.creadaUtc()))
                .toList();
    }

    /** Devuelve una orden por id o lanza RecursoNoEncontradoException. */
    @Transactional(readOnly = true)
    public OrdenCompra obtenerOrden(UUID id) {
        return ordenes.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden de compra no encontrada: " + id));
    }

    /** Aprueba la orden y publica el evento de aprobación con su total. Las reglas de quién puede aprobar están en OrdenCompra.aprobar. */
    @Transactional
    public OrdenCompra aprobarOrden(UUID id, String usuario, boolean esDireccion) {
        OrdenCompra orden = obtenerOrden(id);
        orden.aprobar(usuario, esDireccion, tipoCambioMxnPorUsd, limiteAprobacionUsd);
        OrdenCompra guardada = ordenes.guardar(orden);
        eventos.publicar(new EventoDominio(EVENTO_ORDEN_APROBADA, id.toString(), Map.of(
                "id", id.toString(),
                "proveedor_id", guardada.getProveedorId().toString(),
                "moneda", guardada.getMoneda(),
                "total", guardada.total().toPlainString())));
        return guardada;
    }

    /** Registra una o más líneas recibidas. Si una línea falla, toda la recepción se revierte. */
    @Transactional
    public OrdenCompra registrarRecepcion(UUID id, List<LineaRecibida> lineas) {
        OrdenCompra orden = obtenerOrden(id);
        List<Map<String, Object>> detalle = new java.util.ArrayList<>();
        for (LineaRecibida linea : lineas) {
            Recepcion recepcion = orden.recibir(linea.sku(), linea.cantidad());
            recepciones.guardar(recepcion);
            detalle.add(Map.of("sku", linea.sku(), "cantidad", recepcion.cantidad().toPlainString()));
        }
        OrdenCompra guardada = ordenes.guardar(orden);
        eventos.publicar(new EventoDominio(EVENTO_INSUMOS_RECIBIDOS, id.toString(), Map.of(
                "evento_id", UUID.randomUUID().toString(),
                "orden_id", id.toString(),
                "lineas", detalle)));
        return guardada;
    }

    /** Cancela la orden con su motivo. */
    @Transactional
    public OrdenCompra cancelarOrden(UUID id, String motivo) {
        OrdenCompra orden = obtenerOrden(id);
        orden.cancelar(motivo);
        return ordenes.guardar(orden);
    }
}
