package com.mx.josesaid.develop.pedidos.domain;

import static java.math.BigDecimal.ZERO;
import static java.math.RoundingMode.HALF_EVEN;

import java.math.BigDecimal;
import java.util.List;

/** Subtotal, IVA y total redondeados a los decimales de la moneda con redondeo bancario. */
public record Totales(BigDecimal subtotal, BigDecimal iva, BigDecimal total) {

    public static Totales calcular(List<Linea> lineas, String moneda, TasaIva tasa) {
        int decimales = Monedas.decimales(moneda);
        BigDecimal subtotal = lineas.stream().map(Linea::subtotal).reduce(ZERO, BigDecimal::add)
                .setScale(decimales, HALF_EVEN);
        BigDecimal iva = subtotal.multiply(tasa.valor()).setScale(decimales, HALF_EVEN);
        return new Totales(subtotal, iva, subtotal.add(iva));
    }
}
