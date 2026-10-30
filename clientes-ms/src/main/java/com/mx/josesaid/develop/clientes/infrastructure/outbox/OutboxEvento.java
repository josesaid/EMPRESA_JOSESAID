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

    protected OutboxEvento() {
    }

    public OutboxEvento(UUID id, String tipoAgregado, UUID agregadoId, String tipoEvento, String payload, Instant createdAt) {
        this.id = id;
        this.tipoAgregado = tipoAgregado;
        this.agregadoId = agregadoId;
        this.tipoEvento = tipoEvento;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public void marcarEnviado(Instant ahora) {
        this.enviadoAt = ahora;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgregadoId() {
        return agregadoId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public String getPayload() {
        return payload;
    }
}
