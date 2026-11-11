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

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected OutboxEntity() {
        // requerido por JPA
    }

    /** Crea un evento pendiente de publicar en el outbox. */
    public OutboxEntity(UUID id, String tipo, String agregadoId, String payload, Instant creadoUtc) {
        this.id = id;
        this.tipo = tipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.creadoUtc = creadoUtc;
        this.intentos = 0;
    }

    /** Marca el evento como publicado en la fecha indicada. */
    public void marcarPublicado(Instant ahora) {
        this.publicadoUtc = ahora;
    }

    /** Suma un intento fallido de publicación. */
    public void registrarIntentoFallido() {
        this.intentos++;
    }

    /** Devuelve el identificador del evento. */
    public UUID getId() { return id; }
    /** Devuelve el tipo del evento. */
    public String getTipo() { return tipo; }
    /** Devuelve el id del agregado al que pertenece el evento. */
    public String getAgregadoId() { return agregadoId; }
    /** Devuelve el contenido JSON del evento. */
    public String getPayload() { return payload; }
    /** Devuelve la fecha de creación en UTC. */
    public Instant getCreadoUtc() { return creadoUtc; }
    /** Devuelve la fecha de publicación en UTC, o nula si no se ha publicado. */
    public Instant getPublicadoUtc() { return publicadoUtc; }
    /** Devuelve el número de intentos fallidos. */
    public int getIntentos() { return intentos; }
}
