package com.mx.josesaid.develop.clientes.domain;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Fechas de negocio (alta, registro) en la zona de operación. Los instantes siguen en UTC;
 * solo la fecha calendario se toma en hora de México, para que un alta a las 22:00 cuente como ese día.
 */
public final class FechasNegocio {

    public static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    /** Clase de utilidad; no se instancia. */
    private FechasNegocio() {
    }

    /** Devuelve la fecha de hoy en la zona de México. Así las fechas de alta no cambian de día a mitad de la madrugada por la zona UTC. */
    public static LocalDate hoy(Clock reloj) {
        return LocalDate.now(reloj.withZone(ZONA));
    }
}
