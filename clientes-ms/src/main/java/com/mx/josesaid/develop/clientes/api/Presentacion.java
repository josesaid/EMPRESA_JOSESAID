package com.mx.josesaid.develop.clientes.api;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Convierte instantes UTC a la zona de presentación. Es la única capa que hace esta conversión. */
@Component
public class Presentacion {

    private final ZoneId zona;

    public Presentacion(@Value("${app.zona-presentacion}") String zona) {
        this.zona = ZoneId.of(zona);
    }

    public OffsetDateTime fecha(Instant instante) {
        return instante == null ? null : instante.atZone(zona).toOffsetDateTime();
    }
}
