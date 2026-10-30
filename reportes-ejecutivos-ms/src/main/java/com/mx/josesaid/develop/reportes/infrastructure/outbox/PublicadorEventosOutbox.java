package com.mx.josesaid.develop.reportes.infrastructure.outbox;

import com.mx.josesaid.develop.reportes.application.EventoDominio;
import com.mx.josesaid.develop.reportes.application.PublicadorEventos;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Implementación del patrón outbox: guarda el evento en la base de datos dentro de la
 * transacción de negocio. El envío a Kafka lo hace {@link RetransmisorOutbox}.
 */
@Component
public class PublicadorEventosOutbox implements PublicadorEventos {

    private final OutboxJpaRepository outbox;
    private final ObjectMapper mapper;
    private final Clock reloj;

    public PublicadorEventosOutbox(OutboxJpaRepository outbox, ObjectMapper mapper, Clock reloj) {
        this.outbox = outbox;
        this.mapper = mapper;
        this.reloj = reloj;
    }

    @Override
    public void publicar(EventoDominio evento) {
        String sobre = mapper.writeValueAsString(Map.of(
                "tipo", evento.tipo(),
                "agregadoId", evento.agregadoId(),
                "datos", evento.datos()));
        outbox.save(new OutboxEntity(UUID.randomUUID(), evento.tipo(), evento.agregadoId(),
                sobre, Instant.now(reloj)));
    }
}
