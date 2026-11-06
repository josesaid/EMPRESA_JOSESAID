package com.mx.josesaid.develop.compras.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.Test;

import com.mx.josesaid.develop.compras.api.ComprasControlador.CrearOrdenSolicitud;
import com.mx.josesaid.develop.compras.api.ComprasControlador.LineaRecepcionSolicitud;
import com.mx.josesaid.develop.compras.api.ComprasControlador.LineaSolicitud;
import com.mx.josesaid.develop.compras.api.ComprasControlador.RecepcionSolicitud;

class ValidacionLimitesSolicitudTest {

    private static final Validator VALIDADOR;

    static {
        try (var fabrica = Validation.buildDefaultValidatorFactory()) {
            VALIDADOR = fabrica.getValidator();
        }
    }

    private static LineaSolicitud linea(String cantidad, String precio) {
        return new LineaSolicitud("PAP-A4", new BigDecimal(cantidad), new BigDecimal(precio));
    }

    private static <T> Set<ConstraintViolation<T>> errores(T objeto) {
        return VALIDADOR.validate(objeto);
    }

    @Test
    void aceptaUnaOrdenDentroDeLosLimites() {
        var orden = new CrearOrdenSolicitud(UUID.randomUUID(), "MXN", List.of(linea("10", "85.00")));

        assertThat(errores(orden)).isEmpty();
    }

    @Test
    void rechazaMasDeCienLineas() {
        var lineas = java.util.Collections.nCopies(101, linea("1", "1.00"));
        var orden = new CrearOrdenSolicitud(UUID.randomUUID(), "MXN", lineas);

        assertThat(errores(orden)).isNotEmpty();
    }

    @Test
    void aceptaExactamenteCienLineas() {
        var lineas = java.util.Collections.nCopies(100, linea("1", "1.00"));
        var orden = new CrearOrdenSolicitud(UUID.randomUUID(), "MXN", lineas);

        assertThat(errores(orden)).isEmpty();
    }

    @Test
    void rechazaCantidadConMasDeNueveEnteros() {
        var orden = new CrearOrdenSolicitud(UUID.randomUUID(), "MXN",
                List.of(linea("1000000000", "1.00")));

        assertThat(errores(orden)).isNotEmpty();
    }

    @Test
    void rechazaCantidadConMasDeTresDecimales() {
        var orden = new CrearOrdenSolicitud(UUID.randomUUID(), "MXN",
                List.of(linea("1.2345", "1.00")));

        assertThat(errores(orden)).isNotEmpty();
    }

    @Test
    void rechazaPrecioConMasDeDosDecimales() {
        var orden = new CrearOrdenSolicitud(UUID.randomUUID(), "MXN",
                List.of(linea("1", "85.123")));

        assertThat(errores(orden)).isNotEmpty();
    }

    @Test
    void recepcionRechazaMasDeCienLineas() {
        var lineas = java.util.Collections.nCopies(101,
                new LineaRecepcionSolicitud("PAP-A4", new BigDecimal("1")));

        assertThat(errores(new RecepcionSolicitud(lineas))).isNotEmpty();
    }
}
