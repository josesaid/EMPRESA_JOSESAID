package com.mx.josesaid.develop.clientes.domain;

import com.mx.josesaid.develop.clientes.infrastructure.crypto.CampoCifradoConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** Contacto o evento comercial registrado con un cliente: llamada, correo, reunión, etc. */
@Entity
@Table(name = "interaccion")
public class Interaccion {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoInteraccion tipo;

    @Column(name = "fecha", nullable = false)
    private Instant fecha;

    @Column(name = "responsable_id", nullable = false)
    private UUID responsableId;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "resumen_cifrado", nullable = false)
    private String resumen;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected Interaccion() {
    }

    /** Crea una interacción con su tipo, fecha y resumen. */
    public static Interaccion nueva(UUID id, UUID clienteId, TipoInteraccion tipo, Instant fecha,
                                    UUID responsableId, String resumen, Instant ahora) {
        Interaccion i = new Interaccion();
        i.id = id;
        i.clienteId = clienteId;
        i.tipo = tipo;
        i.fecha = fecha;
        i.responsableId = responsableId;
        i.resumen = resumen.trim();
        i.createdAt = ahora;
        return i;
    }

    /** Devuelve el identificador. */
    public UUID getId() {
        return id;
    }

    /** Devuelve el id del cliente. */
    public UUID getClienteId() {
        return clienteId;
    }

    /** Devuelve el tipo de interacción. */
    public TipoInteraccion getTipo() {
        return tipo;
    }

    /** Devuelve la fecha y hora de la interacción en UTC. */
    public Instant getFecha() {
        return fecha;
    }

    /** Devuelve el id del usuario que la registró. */
    public UUID getResponsableId() {
        return responsableId;
    }

    /** Devuelve el resumen. */
    public String getResumen() {
        return resumen;
    }
}
