package com.mx.josesaid.develop.inventario.infrastructure.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Evento pendiente de publicar a Kafka. Se escribe en la misma transacción que el cambio de negocio. */
@Entity
@Table(name = "outbox_eventos")
public class OutboxEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String tipo;

    @Column(name = "agregado_id", nullable = false, length = 100)
    private String agregadoId;

    /** Sobre JSON completo: tipo, agregadoId y datos. */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "creado_utc", nullable = false)
    private Instant creadoUtc;

    @Column(name = "publicado_utc")
    private Instant publicadoUtc;

    @Column(nullable = false)
    private int intentos;

    protected OutboxEntity() {
        // requerido por JPA
    }

    public OutboxEntity(UUID id, String tipo, String agregadoId, String payload, Instant creadoUtc) {
        this.id = id;
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.creadoUtc = creadoUtc;
        this.intentos = 0;
    }

    public void marcarPublicado(Instant ahora) {
        this.publicadoUtc = ahora;
    }

    public void registrarIntentoFallido() {
        this.intentos++;
    }

    public UUID getId() { return id; }
    public String getTipo() { return tipo; }
    public String getAgregadoId() { return agregadoId; }
    public String getPayload() { return payload; }
    public Instant getCreadoUtc() { return creadoUtc; }
    public Instant getPublicadoUtc() { return publicadoUtc; }
    public int getIntentos() { return intentos; }
}
