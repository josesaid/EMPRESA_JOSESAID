package com.mx.josesaid.develop.inventario.application;

import com.mx.josesaid.develop.inventario.domain.Almacen;
import com.mx.josesaid.develop.inventario.domain.AlmacenRepositorio;
import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import com.mx.josesaid.develop.inventario.domain.ConflictoException;
import com.mx.josesaid.develop.inventario.domain.Existencia;
import com.mx.josesaid.develop.inventario.domain.ExistenciaRepositorio;
import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.MovimientoRepositorio;
import com.mx.josesaid.develop.inventario.domain.OrdenOperacion;
import com.mx.josesaid.develop.inventario.domain.OrdenRepositorio;
import com.mx.josesaid.develop.inventario.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.inventario.domain.SolicitudInvalidaException;
import com.mx.josesaid.develop.inventario.domain.TipoMovimiento;
import com.mx.josesaid.develop.inventario.domain.TipoOrden;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Casos de uso de inventario y operaciones. Los eventos se guardan en el outbox en la misma transacción. */
@Service
public class InventarioService {

    public static final String EVENTO_BAJO_MINIMO = "existencia-bajo-minimo";
    public static final String EVENTO_ORDEN_CERRADA = "orden-operacion-cerrada";

    private final ArticuloRepositorio articulos;
    private final AlmacenRepositorio almacenes;
    private final ExistenciaRepositorio existencias;
    private final MovimientoRepositorio movimientos;
    private final OrdenRepositorio ordenes;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    public InventarioService(ArticuloRepositorio articulos, AlmacenRepositorio almacenes,
                             ExistenciaRepositorio existencias, MovimientoRepositorio movimientos,
                             OrdenRepositorio ordenes, PublicadorEventos eventos, Clock reloj) {
        this.articulos = articulos;
        this.almacenes = almacenes;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.ordenes = ordenes;
        this.eventos = eventos;
        this.reloj = reloj;
    }

    @Transactional
    public Articulo crearArticulo(String sku, String nombre, String unidad, boolean loteObligatorio, BigDecimal minimo) {
        if (sku != null && articulos.existePorSku(sku.trim())) {
            throw new ConflictoException("Ya existe el artículo con SKU " + sku.trim());
        }
        return articulos.guardar(Articulo.crear(sku, nombre, unidad, loteObligatorio, minimo));
    }

    @Transactional
    public Almacen crearAlmacen(String nombre, String ubicacion) {
        return almacenes.guardar(Almacen.crear(nombre, ubicacion));
    }

    /**
     * Registra un movimiento. Orden de validación: artículo y almacén existen (404), lote obligatorio (400),
     * reglas de existencia (409 o 422). Si la existencia queda bajo el mínimo, publica un evento.
     */
    @Transactional
    public MovimientoInventario registrarMovimiento(UUID articuloId, UUID almacenId, TipoMovimiento tipo,
                                                    BigDecimal cantidad, String lote, String referencia) {
        Articulo articulo = obtenerArticulo(articuloId);
        obtenerAlmacen(almacenId);
        if (articulo.isLoteObligatorio() && (lote == null || lote.isBlank())) {
            throw new SolicitudInvalidaException("El artículo " + articulo.getSku() + " requiere lote");
        }
        Existencia existencia = existencias.buscar(articuloId, almacenId)
                .orElseGet(() -> Existencia.vacia(articuloId, almacenId));
        existencia.aplicar(tipo, cantidad);
        existencias.guardar(existencia);

        MovimientoInventario movimiento = movimientos.guardar(
                MovimientoInventario.crear(articuloId, almacenId, tipo, cantidad, lote, referencia, Instant.now(reloj)));

        boolean reduceExistencia = tipo == TipoMovimiento.SALIDA || tipo == TipoMovimiento.AJUSTE;
        if (reduceExistencia && articulo.getMinimo().signum() > 0
                && existencia.getCantidad().compareTo(articulo.getMinimo()) < 0) {
            eventos.publicar(new EventoDominio(EVENTO_BAJO_MINIMO, articuloId.toString(), Map.of(
                    "articulo_id", articuloId.toString(),
                    "almacen_id", almacenId.toString(),
                    "cantidad", existencia.getCantidad().toPlainString())));
        }
        return movimiento;
    }

    @Transactional
    public OrdenOperacion crearOrden(TipoOrden tipo, BigDecimal cantidadObjetivo) {
        return ordenes.guardar(OrdenOperacion.crear(tipo, cantidadObjetivo));
    }

    @Transactional
    public OrdenOperacion cerrarOrden(UUID id, BigDecimal cantidadReal) {
        OrdenOperacion orden = ordenes.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Orden no encontrada: " + id));
        orden.cerrar(cantidadReal);
        OrdenOperacion guardada = ordenes.guardar(orden);
        eventos.publicar(new EventoDominio(EVENTO_ORDEN_CERRADA, id.toString(), Map.of(
                "id", id.toString(),
                "cantidad_real", guardada.getCantidadReal().toPlainString())));
        return guardada;
    }

    @Transactional(readOnly = true)
    public List<Existencia> existenciasDe(UUID articuloId) {
        obtenerArticulo(articuloId);
        return existencias.buscarPorArticulo(articuloId);
    }

    @Transactional(readOnly = true)
    public List<MovimientoInventario> trazabilidad(String lote) {
        List<MovimientoInventario> resultado = movimientos.buscarPorLote(lote);
        if (resultado.isEmpty()) {
            throw new RecursoNoEncontradoException("No hay movimientos para el lote " + lote);
        }
        return resultado;
    }

    private Articulo obtenerArticulo(UUID id) {
        return articulos.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Artículo no encontrado: " + id));
    }

    private Almacen obtenerAlmacen(UUID id) {
        return almacenes.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Almacén no encontrado: " + id));
    }
}
