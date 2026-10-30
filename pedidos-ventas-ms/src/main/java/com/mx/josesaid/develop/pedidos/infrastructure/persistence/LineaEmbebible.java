package com.mx.josesaid.develop.pedidos.infrastructure.persistence;

import com.mx.josesaid.develop.pedidos.domain.Linea;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

/** Línea guardada dentro de la cotización o del pedido. */
@Embeddable
public class LineaEmbebible {

    @Column(name = "producto_id", nullable = false)
    private UUID productoId;

    @Column(nullable = false, length = 200)
    private String descripcion;

    @Column(nullable = false)
    private int cantidad;

    @Column(name = "precio_unitario", nullable = false, precision = 14, scale = 2)
    private BigDecimal precioUnitario;

    protected LineaEmbebible() {
    }

    LineaEmbebible(Linea linea) {
        this.productoId = linea.getProductoId();
        this.descripcion = linea.getDescripcion();
        this.cantidad = linea.getCantidad();
        this.precioUnitario = linea.getPrecioUnitario();
    }

    Linea aLinea() {
        return Linea.reconstruir(productoId, descripcion, cantidad, precioUnitario);
    }
}
