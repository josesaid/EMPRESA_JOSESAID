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

    /** Recibe la zona horaria con la que se muestran las fechas al cliente de la API. */
    public Presentacion(@Value("${app.zona-presentacion}") String zona) {
        this.zona = ZoneId.of(zona);
    }

    /** Convierte un instante UTC a fecha y hora en la zona de presentación. */
    public OffsetDateTime fecha(Instant instante) {
        return instante == null ? null : instante.atZone(zona).toOffsetDateTime();
    }
}
