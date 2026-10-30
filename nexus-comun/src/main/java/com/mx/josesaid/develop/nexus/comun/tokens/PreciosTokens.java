package com.mx.josesaid.develop.nexus.comun.tokens;

import java.math.BigDecimal;

/**
 * Precio por millón de tokens, en la moneda que se configure (por ejemplo USD).
 * Los valores se pasan desde configuración; este módulo no fija precios.
 */
public record PreciosTokens(BigDecimal entradaPorMillon, BigDecimal salidaPorMillon) {

    public static final PreciosTokens SIN_PRECIO = new PreciosTokens(BigDecimal.ZERO, BigDecimal.ZERO);

    public PreciosTokens {
        if (entradaPorMillon == null || salidaPorMillon == null) {
            throw new IllegalArgumentException("Los precios no pueden ser nulos");
        }
        if (entradaPorMillon.signum() < 0 || salidaPorMillon.signum() < 0) {
            throw new IllegalArgumentException("Los precios no pueden ser negativos");
        }
    }

    /** Costo estimado de una llamada, en la misma moneda que los precios. */
    public double costo(long tokensEntrada, long tokensSalida) {
        BigDecimal millon = BigDecimal.valueOf(1_000_000);
        BigDecimal entrada = entradaPorMillon.multiply(BigDecimal.valueOf(tokensEntrada)).divide(millon);
        BigDecimal salida = salidaPorMillon.multiply(BigDecimal.valueOf(tokensSalida)).divide(millon);
        return entrada.add(salida).doubleValue();
    }
}
