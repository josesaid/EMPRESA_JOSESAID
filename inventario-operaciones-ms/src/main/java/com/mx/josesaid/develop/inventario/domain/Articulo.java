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

    /** Constructor privado: los artículos se crean con sus fábricas. */
    private Articulo(UUID id, String sku, String nombre, String unidad, boolean loteObligatorio, BigDecimal minimo) {
        this.id = id;
        this.sku = sku;
        this.nombre = nombre;
        this.unidad = unidad;
        this.loteObligatorio = loteObligatorio;
        this.minimo = minimo;
    }

    /** Crea un artículo nuevo. Valida SKU, nombre, unidad y mínimo. */
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

    /** Reconstruye un artículo desde la base de datos sin aplicar reglas. */
    public static Articulo reconstruir(UUID id, String sku, String nombre, String unidad, boolean loteObligatorio,
                                       BigDecimal minimo) {
        return new Articulo(id, sku, nombre, unidad, loteObligatorio, minimo);
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el SKU. */
    public String getSku() { return sku; }
    /** Devuelve el nombre. */
    public String getNombre() { return nombre; }
    /** Devuelve la unidad de medida. */
    public String getUnidad() { return unidad; }
    /** Indica si el artículo exige lote en cada movimiento. */
    public boolean isLoteObligatorio() { return loteObligatorio; }
    /** Devuelve la existencia mínima; por debajo de ella el artículo aparece en alertas. */
    public BigDecimal getMinimo() { return minimo; }
}
