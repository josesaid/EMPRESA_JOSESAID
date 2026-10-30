package com.mx.josesaid.develop.pedidos.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Línea de cotización o pedido. Precio y tasa quedan congelados al confirmar el pedido. */
public final class Linea {

    private final UUID productoId;
    private final String descripcion;
    private final int cantidad;
    private final BigDecimal precioUnitario;

    private Linea(UUID productoId, String descripcion, int cantidad, BigDecimal precioUnitario) {
        this.productoId = productoId;
        this.descripcion = descripcion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public static Linea crear(UUID productoId, String descripcion, int cantidad, BigDecimal precioUnitario) {
        if (productoId == null) {
            throw new ReglaDeNegocioException("Cada línea requiere producto");
        }
        if (cantidad <= 0) {
            throw new ReglaDeNegocioException("La cantidad de cada línea debe ser mayor a cero");
        }
        if (precioUnitario == null || precioUnitario.signum() < 0) {
            throw new ReglaDeNegocioException("El precio unitario no puede ser negativo");
        }
        return new Linea(productoId, descripcion == null ? "" : descripcion.trim(), cantidad, precioUnitario);
    }

    public static Linea reconstruir(UUID productoId, String descripcion, int cantidad, BigDecimal precioUnitario) {
        return new Linea(productoId, descripcion, cantidad, precioUnitario);
    }

    public BigDecimal subtotal() {
        return precioUnitario.multiply(BigDecimal.valueOf(cantidad));
    }

    public UUID getProductoId() { return productoId; }
    public String getDescripcion() { return descripcion; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
}
