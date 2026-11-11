package com.mx.josesaid.develop.inventario.domain;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Existencia de un artículo en un almacén. Reglas: nunca negativa, lo reservado nunca supera
 * lo existente y una salida o reserva no puede exceder lo disponible (existencia menos reserva).
 */
public class Existencia {

    private final UUID articuloId;
    private final UUID almacenId;
    private BigDecimal cantidad;
    private BigDecimal reservado;

    /** Constructor privado: las existencias se crean con sus fábricas. */
    private Existencia(UUID articuloId, UUID almacenId, BigDecimal cantidad, BigDecimal reservado) {
        this.articuloId = articuloId;
        this.almacenId = almacenId;
        this.cantidad = cantidad;
        this.reservado = reservado;
    }

    /** Crea una existencia en cero para un artículo en un almacén. */
    public static Existencia vacia(UUID articuloId, UUID almacenId) {
        return new Existencia(articuloId, almacenId, ZERO, ZERO);
    }

    /** Reconstruye una existencia desde la base de datos sin aplicar reglas. */
    public static Existencia reconstruir(UUID articuloId, UUID almacenId, BigDecimal cantidad, BigDecimal reservado) {
        return new Existencia(articuloId, almacenId, cantidad, reservado);
    }

    /** Aplica un movimiento. Valida cantidad y reglas de existencia; lanza 409 si no alcanza. */
    public void aplicar(TipoMovimiento tipo, BigDecimal cantidadMov) {
        BigDecimal c = MovimientoInventario.validarCantidad(tipo, cantidadMov);
        switch (tipo) {
            case ENTRADA -> cantidad = cantidad.add(c);
            case SALIDA -> {
                exigirDisponible(c);
                cantidad = cantidad.subtract(c);
            }
            case RESERVA -> {
                exigirDisponible(c);
                reservado = reservado.add(c);
            }
            case AJUSTE -> {
                BigDecimal nueva = cantidad.add(c);
                if (nueva.signum() < 0 || nueva.compareTo(reservado) < 0) {
                    throw new ConflictoException("El ajuste dejaría la existencia negativa o por debajo de lo reservado");
                }
                cantidad = nueva;
            }
        }
    }

    /** Libera parte de lo reservado, por ejemplo al entregar el pedido que la reservó. */
    public void liberarReserva(BigDecimal cantidadLiberada) {
        BigDecimal c = MovimientoInventario.validarCantidad(TipoMovimiento.RESERVA, cantidadLiberada);
        if (c.compareTo(reservado) > 0) {
            throw new ConflictoException("No se puede liberar más de lo reservado: reservado "
                    + reservado.toPlainString() + ", solicitado " + c.toPlainString());
        }
        reservado = reservado.subtract(c);
    }

    /** Devuelve lo disponible: existencia menos lo reservado. */
    public BigDecimal disponible() {
        return cantidad.subtract(reservado);
    }

    /** Rechaza la operación si la cantidad pedida supera lo disponible. */
    private void exigirDisponible(BigDecimal c) {
        if (c.compareTo(disponible()) > 0) {
            throw new ConflictoException("Existencia insuficiente: disponible " + disponible().toPlainString()
                    + ", solicitado " + c.toPlainString());
        }
    }

    /** Devuelve el id del artículo. */
    public UUID getArticuloId() { return articuloId; }
    /** Devuelve el id del almacén. */
    public UUID getAlmacenId() { return almacenId; }
    /** Devuelve la cantidad total existente. */
    public BigDecimal getCantidad() { return cantidad; }
    /** Devuelve la cantidad reservada. */
    public BigDecimal getReservado() { return reservado; }
}
