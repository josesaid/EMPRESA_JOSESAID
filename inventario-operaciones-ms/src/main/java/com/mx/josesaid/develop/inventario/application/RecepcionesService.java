package com.mx.josesaid.develop.inventario.application;

import com.mx.josesaid.develop.inventario.domain.Articulo;
import com.mx.josesaid.develop.inventario.domain.ArticuloRepositorio;
import com.mx.josesaid.develop.inventario.domain.Existencia;
import com.mx.josesaid.develop.inventario.domain.ExistenciaRepositorio;
import com.mx.josesaid.develop.inventario.domain.MovimientoInventario;
import com.mx.josesaid.develop.inventario.domain.MovimientoRepositorio;
import com.mx.josesaid.develop.inventario.domain.RecepcionesRepositorio;
import com.mx.josesaid.develop.inventario.domain.TipoMovimiento;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Suma a las existencias lo recibido de una orden de compra. Cada recepción se aplica una sola vez.
 * Los artículos con lote obligatorio no se registran: el evento de compras no trae lote.
 */
@Service
public class RecepcionesService {

    private static final Logger log = LoggerFactory.getLogger(RecepcionesService.class);

    /**
     * Línea recibida: SKU del artículo y cantidad.
     *
     * @param sku clave SKU del artículo, de tipo {@code String}.
     * @param cantidad cantidad solicitada o registrada, de tipo {@code BigDecimal}.
     */
    public record LineaRecibida(String sku, BigDecimal cantidad) {
    }

    private final ArticuloRepositorio articulos;
    private final ExistenciaRepositorio existencias;
    private final MovimientoRepositorio movimientos;
    private final RecepcionesRepositorio recepciones;
    private final Clock reloj;

    /** Recibe los repositorios, el reloj y el registro de recepciones ya aplicadas. */
    public RecepcionesService(ArticuloRepositorio articulos, ExistenciaRepositorio existencias,
                              MovimientoRepositorio movimientos, RecepcionesRepositorio recepciones, Clock reloj) {
        this.articulos = articulos;
        this.existencias = existencias;
        this.movimientos = movimientos;
        this.recepciones = recepciones;
        this.reloj = reloj;
    }

    /** Suma a las existencias lo recibido de una orden de compra. Cada recepción se aplica una sola vez, aunque el evento llegue dos veces. Los artículos con lote obligatorio no se registran, porque el evento no trae lote. */
    @Transactional
    public void registrar(UUID eventoId, UUID ordenId, UUID almacenId, List<LineaRecibida> lineas) {
        if (!recepciones.marcarProcesada(eventoId)) {
            return;
        }
        Instant ahora = Instant.now(reloj);
        for (LineaRecibida linea : lineas) {
            Optional<Articulo> encontrado = articulos.buscarPorSku(linea.sku());
            if (encontrado.isEmpty()) {
                log.warn("Orden {}: el SKU {} no existe en inventario; no se registra la entrada", ordenId, linea.sku());
                continue;
            }
            Articulo articulo = encontrado.get();
            if (articulo.isLoteObligatorio()) {
                log.error("Orden {}: el artículo {} requiere lote y el evento no lo trae; no se registra la entrada",
                        ordenId, linea.sku());
                continue;
            }
            Existencia existencia = existencias.buscar(articulo.getId(), almacenId)
                    .orElseGet(() -> Existencia.vacia(articulo.getId(), almacenId));
            existencia.aplicar(TipoMovimiento.ENTRADA, linea.cantidad());
            existencias.guardar(existencia);
            movimientos.guardar(MovimientoInventario.crear(articulo.getId(), almacenId, TipoMovimiento.ENTRADA,
                    linea.cantidad(), null, "Recepción orden " + ordenId, ahora));
        }
    }
}
