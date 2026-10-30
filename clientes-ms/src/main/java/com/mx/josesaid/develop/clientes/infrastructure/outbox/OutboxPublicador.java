package com.mx.josesaid.develop.clientes.infrastructure.outbox;

import java.time.Clock;
import java.time.Instant;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Publica los eventos pendientes hacia Kafka. Garantía: al menos una vez.
 * Se detiene en el primer fallo del lote para conservar el orden; el resto se reintenta en el siguiente ciclo.
 */
@Component
public class OutboxPublicador {

    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublicador.class);
    private static final long ESPERA_ENVIO_SEGUNDOS = 5;

    private final OutboxRepository repositorio;
    private final KafkaTemplate<String, String> kafka;
    private final String topico;
    private final Clock reloj;

    public OutboxPublicador(OutboxRepository repositorio,
                            KafkaTemplate<String, String> kafka,
                            @Value("${app.eventos.topico}") String topico,
                            Clock reloj) {
        this.repositorio = repositorio;
        this.kafka = kafka;
        this.topico = topico;
        this.reloj = reloj;
    }

    @Scheduled(fixedDelayString = "${app.eventos.intervalo-publicacion-ms}")
    @Transactional
    public void publicarPendientes() {
        for (OutboxEvento evento : repositorio.findTop100ByEnviadoAtIsNullOrderByCreatedAtAsc()) {
            try {
                kafka.send(topico, evento.getAgregadoId().toString(), evento.getPayload())
                        .get(ESPERA_ENVIO_SEGUNDOS, TimeUnit.SECONDS);
                evento.marcarEnviado(Instant.now(reloj));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Publicación interrumpida; se reintenta en el siguiente ciclo");
                return;
            } catch (ExecutionException | TimeoutException e) {
                LOG.warn("Evento pendiente de publicar, se reintenta. eventoId={} tipo={}",
                        evento.getId(), evento.getTipoEvento());
                return;
            }
        }
    }
}
