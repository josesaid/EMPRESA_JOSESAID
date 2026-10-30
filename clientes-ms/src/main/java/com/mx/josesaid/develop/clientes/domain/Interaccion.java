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

    protected Interaccion() {
    }

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

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public TipoInteraccion getTipo() {
        return tipo;
    }

    public Instant getFecha() {
        return fecha;
    }

    public UUID getResponsableId() {
        return responsableId;
    }

    public String getResumen() {
        return resumen;
    }
}
