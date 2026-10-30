package com.mx.josesaid.develop.clientes.application;

import com.mx.josesaid.develop.clientes.infrastructure.outbox.OutboxEvento;
import com.mx.josesaid.develop.clientes.infrastructure.outbox.OutboxRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Escribe el evento en la tabla outbox. Debe llamarse dentro de la misma transacción que el cambio de estado.
 * El sobre del evento incluye eventoId, tipoEvento y ocurridoEn, además de los datos propios del evento.
 */
@Component
public class RegistradorEventos {

    private final OutboxRepository outbox;
    private final ObjectMapper json;
    private final Clock reloj;

    public RegistradorEventos(OutboxRepository outbox, ObjectMapper json, Clock reloj) {
        this.outbox = outbox;
        this.json = json;
        this.reloj = reloj;
    }

    public void registrar(String tipoAgregado, UUID agregadoId, String tipoEvento, Map<String, Object> datos) {
        UUID eventoId = UUID.randomUUID();
        Instant ahora = Instant.now(reloj);

        Map<String, Object> sobre = new LinkedHashMap<>();
        sobre.put("eventoId", eventoId.toString());
        sobre.put("tipoEvento", tipoEvento);
        sobre.put("ocurridoEn", ahora.toString());
        sobre.putAll(datos);

        String payload = json.writeValueAsString(sobre);
        outbox.save(new OutboxEvento(eventoId, tipoAgregado, agregadoId, tipoEvento, payload, ahora));
    }
}
