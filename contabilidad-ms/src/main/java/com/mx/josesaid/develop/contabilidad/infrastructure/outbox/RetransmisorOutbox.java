package com.mx.josesaid.develop.contabilidad.infrastructure.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Envía a Kafka los eventos pendientes del outbox.
 *
 * <p>Se detiene en el primer fallo para conservar el orden de los eventos.
 * Un evento con error queda pendiente y se reintenta en el siguiente ciclo.
 */
@Component
public class RetransmisorOutbox {

    /** Tópico donde se publican los eventos de identidad. */
    public static final String TOPICO = "contabilidad.eventos";

    private final OutboxJpaRepository outbox;
    private final KafkaTemplate<String, String> kafka;
    private final Clock reloj;

    public RetransmisorOutbox(OutboxJpaRepository outbox, KafkaTemplate<String, String> kafka, Clock reloj) {
        this.outbox = outbox;
        this.kafka = kafka;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${contabilidad.outbox.intervalo-ms:5000}")
    @Transactional
    public void transmitir() {
        for (OutboxEntity evento : outbox.findTop100ByPublicadoUtcIsNullOrderByCreadoUtcAsc()) {
            try {
                kafka.send(TOPICO, evento.getAgregadoId(), evento.getPayload()).get(10, TimeUnit.SECONDS);
                evento.marcarPublicado(Instant.now(reloj));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            } catch (Exception ex) {
                evento.registrarIntentoFallido();
                return;
            }
        }
    }
}
