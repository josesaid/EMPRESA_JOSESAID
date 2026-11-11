package com.mx.josesaid.develop.inventario.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/** Orden de producción o de servicio. Solo se cierra con cantidad real mayor a cero. */
public class OrdenOperacion {

    private final UUID id;
    private final TipoOrden tipo;
    private EstatusOrden estatus;
    private final BigDecimal cantidadObjetivo;
    private BigDecimal cantidadReal;

    /** Constructor privado: las órdenes se crean con sus fábricas. */
    private OrdenOperacion(UUID id, TipoOrden tipo, EstatusOrden estatus, BigDecimal cantidadObjetivo,
                           BigDecimal cantidadReal) {
        this.id = id;
        this.tipo = tipo;
        this.estatus = estatus;
        this.cantidadObjetivo = cantidadObjetivo;
        this.cantidadReal = cantidadReal;
    }

    /** Crea una orden abierta. La cantidad objetivo debe ser mayor a cero. */
    public static OrdenOperacion crear(TipoOrden tipo, BigDecimal cantidadObjetivo) {
        if (tipo == null) {
            throw new ReglaDeNegocioException("El tipo de orden es obligatorio");
        }
        if (cantidadObjetivo == null || cantidadObjetivo.signum() <= 0) {
            throw new ReglaDeNegocioException("La cantidad objetivo debe ser mayor a cero");
        }
        return new OrdenOperacion(UUID.randomUUID(), tipo, EstatusOrden.ABIERTA,
                cantidadObjetivo.setScale(3, RoundingMode.HALF_EVEN), null);
    }

    /** Reconstruye una orden desde la base de datos sin aplicar reglas. */
    public static OrdenOperacion reconstruir(UUID id, TipoOrden tipo, EstatusOrden estatus, BigDecimal objetivo,
                                             BigDecimal real) {
        return new OrdenOperacion(id, tipo, estatus, objetivo, real);
    }

    /** Cierra la orden con la cantidad real. Una orden cerrada no se vuelve a cerrar; la cantidad real debe ser mayor a cero. */
    public void cerrar(BigDecimal real) {
        if (estatus == EstatusOrden.CERRADA) {
            throw new ConflictoException("La orden ya está cerrada");
        }
        if (real == null || real.signum() <= 0) {
            throw new ReglaDeNegocioException("Cerrar una orden requiere la cantidad producida o el servicio completado");
        }
        this.cantidadReal = real.setScale(3, RoundingMode.HALF_EVEN);
        this.estatus = EstatusOrden.CERRADA;
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Devuelve el tipo de orden. */
    public TipoOrden getTipo() { return tipo; }
    /** Devuelve el estatus. */
    public EstatusOrden getEstatus() { return estatus; }
    /** Devuelve la cantidad objetivo. */
    public BigDecimal getCantidadObjetivo() { return cantidadObjetivo; }
    /** Devuelve la cantidad real, o nula si la orden sigue abierta. */
    public BigDecimal getCantidadReal() { return cantidadReal; }
}
