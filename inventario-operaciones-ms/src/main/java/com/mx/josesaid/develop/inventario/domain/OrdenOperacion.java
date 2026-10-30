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

    private OrdenOperacion(UUID id, TipoOrden tipo, EstatusOrden estatus, BigDecimal cantidadObjetivo,
                           BigDecimal cantidadReal) {
        this.id = id;
        this.tipo = tipo;
        this.estatus = estatus;
        this.cantidadObjetivo = cantidadObjetivo;
        this.cantidadReal = cantidadReal;
    }

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

    public static OrdenOperacion reconstruir(UUID id, TipoOrden tipo, EstatusOrden estatus, BigDecimal objetivo,
                                             BigDecimal real) {
        return new OrdenOperacion(id, tipo, estatus, objetivo, real);
    }

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

    public UUID getId() { return id; }
    public TipoOrden getTipo() { return tipo; }
    public EstatusOrden getEstatus() { return estatus; }
    public BigDecimal getCantidadObjetivo() { return cantidadObjetivo; }
    public BigDecimal getCantidadReal() { return cantidadReal; }
}
