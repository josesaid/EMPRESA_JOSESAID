package com.mx.josesaid.develop.clientes.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Evento pendiente de publicar. Se escribe en la misma transacción que el cambio de estado. */
@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {

    @Id
    private UUID id;

    @Column(name = "tipo_agregado", nullable = false, length = 50)
    private String tipoAgregado;

    @Column(name = "agregado_id", nullable = false)
    private UUID agregadoId;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "enviado_at")
    private Instant enviadoAt;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected OutboxEvento() {
    }

    /** Crea un evento pendiente de publicar en el outbox. */
    public OutboxEvento(UUID id, String tipoAgregado, UUID agregadoId, String tipoEvento, String payload, Instant createdAt) {
        this.id = id;
        this.tipoAgregado = tipoAgregado;
        this.agregadoId = agregadoId;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    /** Marca el evento como enviado en la fecha indicada. */
    public void marcarEnviado(Instant ahora) {
        this.enviadoAt = ahora;
    }

    /** Devuelve el identificador del evento. */
    public UUID getId() {
        return id;
    }

    /** Devuelve el id del agregado al que pertenece el evento. */
    public UUID getAgregadoId() {
        return agregadoId;
    }

    /** Devuelve el tipo del evento. */
    public String getTipoEvento() {
        return tipoEvento;
    }

    /** Devuelve el contenido JSON del evento. */
    public String getPayload() {
        return payload;
    }
}
