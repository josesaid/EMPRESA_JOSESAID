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

    public UUID getId() { return id; }
    public UUID getArticuloId() { return articuloId; }
    public UUID getAlmacenId() { return almacenId; }
    public TipoMovimiento getTipo() { return tipo; }
    public BigDecimal getCantidad() { return cantidad; }
    public String getLote() { return lote; }
    public String getReferencia() { return referencia; }
    public Instant getCreadoUtc() { return creadoUtc; }
}
