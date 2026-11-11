package com.mx.josesaid.develop.inventario.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.UUID;

/** Movimiento inmutable de inventario. Se compensa con otro movimiento; nunca se borra. */
public final class MovimientoInventario {

    private final UUID id;
    private final UUID articuloId;
    private final UUID almacenId;
    private final TipoMovimiento tipo;
    private final BigDecimal cantidad;
    private final String lote;
    private final String referencia;
    private final Instant creadoUtc;

    /** Constructor privado: los movimientos se crean con sus fábricas. */
    private MovimientoInventario(UUID id, UUID articuloId, UUID almacenId, TipoMovimiento tipo, BigDecimal cantidad,
                                 String lote, String referencia, Instant creadoUtc) {
        this.id = id;
        this.articuloId = articuloId;
        this.almacenId = almacenId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.lote = lote;
        this.referencia = referencia;
        this.creadoUtc = creadoUtc;
    }

    /** Crea un movimiento nuevo y valida su cantidad según el tipo. */
    public static MovimientoInventario crear(UUID articuloId, UUID almacenId, TipoMovimiento tipo, BigDecimal cantidad,
                                             String lote, String referencia, Instant creadoUtc) {
        if (articuloId == null || almacenId == null) {
            throw new ReglaDeNegocioException("El artículo y el almacén son obligatorios");
        }
        if (tipo == null) {
            throw new ReglaDeNegocioException("El tipo de movimiento es obligatorio");
        }
        BigDecimal c = validarCantidad(tipo, cantidad);
        String loteLimpio = lote == null || lote.isBlank() ? null : lote.trim();
        String refLimpia = referencia == null || referencia.isBlank() ? null : referencia.trim();
        return new MovimientoInventario(UUID.randomUUID(), articuloId, almacenId, tipo, c, loteLimpio, refLimpia, creadoUtc);
    }

    /** Reconstruye un movimiento desde la base de datos sin aplicar reglas. */
    public static MovimientoInventario reconstruir(UUID id, UUID articuloId, UUID almacenId, TipoMovimiento tipo,
                                                   BigDecimal cantidad, String lote, String referencia, Instant creadoUtc) {
        return new MovimientoInventario(id, articuloId, almacenId, tipo, cantidad, lote, referencia, creadoUtc);
    }

    /** Cantidades con tres decimales. AJUSTE admite negativos; los demás tipos deben ser positivos. */
    static BigDecimal validarCantidad(TipoMovimiento tipo, BigDecimal cantidad) {
        if (cantidad == null) {
            throw new ReglaDeNegocioException("La cantidad es obligatoria");
        }
        BigDecimal c = cantidad.setScale(3, RoundingMode.HALF_EVEN);
        if (tipo == TipoMovimiento.AJUSTE) {
            if (c.signum() == 0) {
                throw new ReglaDeNegocioException("El ajuste no puede ser cero");
            }
        } else if (c.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad debe ser mayor a cero");
        }
        return c;
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el id del artículo. */
    public UUID getArticuloId() { return articuloId; }
    /** Devuelve el id del almacén. */
    public UUID getAlmacenId() { return almacenId; }
    /** Devuelve el tipo de movimiento. */
    public TipoMovimiento getTipo() { return tipo; }
    /** Devuelve la cantidad. */
    public BigDecimal getCantidad() { return cantidad; }
    /** Devuelve el lote, si aplica. */
    public String getLote() { return lote; }
    /** Devuelve la referencia que explica el movimiento, por ejemplo el pedido o la orden. */
    public String getReferencia() { return referencia; }
    /** Devuelve el instante de creación en UTC. */
    public Instant getCreadoUtc() { return creadoUtc; }
}
