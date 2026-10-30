package com.mx.josesaid.develop.compras.domain;

import static java.math.BigDecimal.ZERO;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Línea de una orden: lo ordenado, lo recibido hasta ahora y el precio pactado. */
public final class LineaOrden {

    private final String articuloSku;
    private final BigDecimal cantidadOrdenada;
    private final BigDecimal cantidadRecibida;
    private final BigDecimal precioUnitario;

    private LineaOrden(String articuloSku, BigDecimal cantidadOrdenada, BigDecimal cantidadRecibida,
                       BigDecimal precioUnitario) {
        this.articuloSku = articuloSku;
        this.cantidadOrdenada = cantidadOrdenada;
        this.cantidadRecibida = cantidadRecibida;
        this.precioUnitario = precioUnitario;
    }

    public static LineaOrden nueva(String sku, BigDecimal cantidad, BigDecimal precioUnitario) {
        if (sku == null || sku.isBlank()) {
            throw new ReglaDeNegocioException("Cada línea requiere SKU");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad de cada línea debe ser mayor a cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new ReglaDeNegocioException("El precio unitario no puede ser negativo");
        }
        return new LineaOrden(sku.trim(), cantidad.setScale(3, RoundingMode.HALF_EVEN), ZERO,
                precioUnitario.setScale(2, RoundingMode.HALF_EVEN));
    }

    public static LineaOrden reconstruir(String sku, BigDecimal ordenada, BigDecimal recibida, BigDecimal precio) {
        return new LineaOrden(sku, ordenada, recibida, precio);
    }

    /** Devuelve una copia con la cantidad recibida incrementada. */
    LineaOrden conRecepcion(BigDecimal cantidad) {
        return new LineaOrden(articuloSku, cantidadOrdenada, cantidadRecibida.add(cantidad), precioUnitario);
    }

    public BigDecimal subtotal() {
        return cantidadOrdenada.multiply(precioUnitario).setScale(2, RoundingMode.HALF_EVEN);
    }

    public boolean completa() {
        return cantidadRecibida.compareTo(cantidadOrdenada) >= 0;
    }

    public String getArticuloSku() { return articuloSku; }
    public BigDecimal getCantidadOrdenada() { return cantidadOrdenada; }
    public BigDecimal getCantidadRecibida() { return cantidadRecibida; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
