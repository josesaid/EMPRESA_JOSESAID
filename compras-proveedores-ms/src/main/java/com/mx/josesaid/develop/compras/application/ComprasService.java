package com.mx.josesaid.develop.compras.application;

import com.mx.josesaid.develop.compras.domain.ConflictoException;
import com.mx.josesaid.develop.compras.domain.LineaOrden;
import com.mx.josesaid.develop.compras.domain.OrdenCompra;
import com.mx.josesaid.develop.compras.domain.OrdenCompraRepositorio;
import com.mx.josesaid.develop.compras.domain.Proveedor;
import com.mx.josesaid.develop.compras.domain.ProveedorRepositorio;
import com.mx.josesaid.develop.compras.domain.Recepcion;
import com.mx.josesaid.develop.compras.domain.RecepcionRepositorio;
import com.mx.josesaid.develop.compras.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.compras.domain.SolicitudInvalidaException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de compras. Cada cambio de estado relevante se publica por outbox en la misma transacción. */
@Service
public class ComprasService {

    public static final String EVENTO_ORDEN_APROBADA = "orden-compra-aprobada";
    public static final String EVENTO_INSUMOS_RECIBIDOS = "insumos-recibidos";

    private final ProveedorRepositorio proveedores;
    private final OrdenCompraRepositorio ordenes;
    private final RecepcionRepositorio recepciones;
    private final PublicadorEventos eventos;

    public ComprasService(ProveedorRepositorio proveedores, OrdenCompraRepositorio ordenes,
                          RecepcionRepositorio recepciones, PublicadorEventos eventos) {
        this.proveedores = proveedores;
        this.ordenes = ordenes;
        this.recepciones = recepciones;
        this.eventos = eventos;
    }

    @Transactional
    public Proveedor crearProveedor(String razonSocial, String rfc, String correo) {
        Proveedor proveedor = Proveedor.crear(razonSocial, rfc, correo);
        if (proveedores.existePorRfc(proveedor.getRfc())) {
            throw new ConflictoException("Ya existe un proveedor con ese RFC");
        }
        return proveedores.guardar(proveedor);
    }

    @Transactional(readOnly = true)
    public List<Proveedor> buscarProveedores(String texto) {
        String termino = texto == null ? "" : texto.trim();
        if (termino.length() < 3) {
            throw new SolicitudInvalidaException("La búsqueda requiere al menos 3 caracteres");
        }
        return proveedores.buscarPorRazonSocial(termino);
    }

    public Proveedor obtenerProveedor(UUID id) {
        return proveedores.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Proveedor no encontrado: " + id));
    }

    @Transactional
    public OrdenCompra crearOrden(UUID proveedorId, String moneda, List<LineaOrden> lineas) {
        Proveedor proveedor = obtenerProveedor(proveedorId);
        return ordenes.guardar(OrdenCompra.crear(proveedor, moneda, lineas));
    }

    @Transactional(readOnly = true)
    public OrdenCompra obtenerOrden(UUID id) {
        return ordenes.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden de compra no encontrada: " + id));
    }

    @Transactional
    public OrdenCompra aprobarOrden(UUID id) {
        OrdenCompra orden = obtenerOrden(id);
        orden.aprobar();
        OrdenCompra guardada = ordenes.guardar(orden);
        eventos.publicar(new EventoDominio(EVENTO_ORDEN_APROBADA, id.toString(), Map.of(
                "id", id.toString(),
                "proveedor_id", guardada.getProveedorId().toString(),
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
                "orden_id", id.toString(),
                "lineas", detalle)));
        return guardada;
    }

    @Transactional
    public OrdenCompra cancelarOrden(UUID id, String motivo) {
        OrdenCompra orden = obtenerOrden(id);
        orden.cancelar(motivo);
        return ordenes.guardar(orden);
    }
}
