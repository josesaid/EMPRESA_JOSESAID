package com.mx.josesaid.develop.inventario.domain;

import java.math.BigDecimal;
import java.util.UUID;

/** Artículo del catálogo. Si requiere lote, todo movimiento debe indicarlo. */
public class Articulo {

    private final UUID id;
    private final String sku;
    private final String nombre;
    private final String unidad;
    private final boolean loteObligatorio;
    private final BigDecimal minimo;

    private Articulo(UUID id, String sku, String nombre, String unidad, boolean loteObligatorio, BigDecimal minimo) {
        this.id = id;
        this.sku = sku;
        this.nombre = nombre;
        this.unidad = unidad;
        this.loteObligatorio = loteObligatorio;
        this.minimo = minimo;
    }

    public static Articulo crear(String sku, String nombre, String unidad, boolean loteObligatorio, BigDecimal minimo) {
        if (sku == null || sku.isBlank() || sku.length() > 40) {
            throw new ReglaDeNegocioException("El SKU es obligatorio (máximo 40 caracteres)");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new ReglaDeNegocioException("El nombre del artículo es obligatorio");
        }
        if (unidad == null || unidad.isBlank() || unidad.length() > 10) {
            throw new ReglaDeNegocioException("La unidad de medida es obligatoria (máximo 10 caracteres)");
        }
        BigDecimal min = minimo == null ? BigDecimal.ZERO : minimo;
        if (min.signum() < 0) {
            throw new ReglaDeNegocioException("El mínimo no puede ser negativo");
        }
        return new Articulo(UUID.randomUUID(), sku.trim(), nombre.trim(), unidad.trim(), loteObligatorio,
                min.setScale(3, java.math.RoundingMode.HALF_EVEN));
    }

    public static Articulo reconstruir(UUID id, String sku, String nombre, String unidad, boolean loteObligatorio,
                                       BigDecimal minimo) {
        return new Articulo(id, sku, nombre, unidad, loteObligatorio, minimo);
    }

    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getNombre() { return nombre; }
    public String getUnidad() { return unidad; }
    public boolean isLoteObligatorio() { return loteObligatorio; }
    public BigDecimal getMinimo() { return minimo; }
}
