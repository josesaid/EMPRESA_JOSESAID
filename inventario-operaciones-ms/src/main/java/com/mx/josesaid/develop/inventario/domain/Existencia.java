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

    private Existencia(UUID articuloId, UUID almacenId, BigDecimal cantidad, BigDecimal reservado) {
        this.articuloId = articuloId;
        this.almacenId = almacenId;
        this.cantidad = cantidad;
        this.reservado = reservado;
    }

    public static Existencia vacia(UUID articuloId, UUID almacenId) {
        return new Existencia(articuloId, almacenId, ZERO, ZERO);
    }

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

    public BigDecimal disponible() {
        return cantidad.subtract(reservado);
    }

    private void exigirDisponible(BigDecimal c) {
        if (c.compareTo(disponible()) > 0) {
            throw new ConflictoException("Existencia insuficiente: disponible " + disponible().toPlainString()
                    + ", solicitado " + c.toPlainString());
        }
    }

    public UUID getArticuloId() { return articuloId; }
    public UUID getAlmacenId() { return almacenId; }
    public BigDecimal getCantidad() { return cantidad; }
    public BigDecimal getReservado() { return reservado; }
}
