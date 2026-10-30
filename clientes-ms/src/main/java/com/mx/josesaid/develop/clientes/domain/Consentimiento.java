package com.mx.josesaid.develop.clientes.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "consentimiento")
public class Consentimiento {

    @Id
    private UUID id;

    @Column(name = "contacto_id", nullable = false)
    private UUID contactoId;

    @Column(name = "finalidad", nullable = false, length = 100)
    private String finalidad;

    @Column(name = "fecha_otorgado", nullable = false)
    private Instant fechaOtorgado;

    @Enumerated(EnumType.STRING)
    @Column(name = "medio", nullable = false, length = 20)
    private MedioConsentimiento medio;

    @Column(name = "vigencia")
    private LocalDate vigencia;

    @Column(name = "revocado_at")
    private Instant revocadoAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Consentimiento() {
    }

    public static Consentimiento otorgado(UUID id, UUID contactoId, String finalidad, Instant fechaOtorgado,
                                          MedioConsentimiento medio, LocalDate vigencia, Instant ahora) {
        Consentimiento c = new Consentimiento();
        c.id = id;
        c.contactoId = contactoId;
        c.finalidad = finalidad.trim();
        c.fechaOtorgado = fechaOtorgado;
        c.medio = medio;
        c.vigencia = vigencia;
        c.createdAt = ahora;
        return c;
    }

    /**
     * Revoca el consentimiento. Devuelve true solo la primera vez; las siguientes son no-op.
     */
    public boolean revocar(Instant ahora) {
        if (revocadoAt != null) {
            return false;
        }
        revocadoAt = ahora;
        return true;
    }

    /** Vigente si no fue revocado y su vigencia (si existe) no ha vencido. */
    public boolean estaVigente(LocalDate hoy) {
        return revocadoAt == null && (vigencia == null || !hoy.isAfter(vigencia));
    }

    public UUID getId() {
        return id;
    }

    public UUID getContactoId() {
        return contactoId;
    }

    public String getFinalidad() {
        return finalidad;
    }

    public Instant getFechaOtorgado() {
        return fechaOtorgado;
    }

    public MedioConsentimiento getMedio() {
        return medio;
    }

    public LocalDate getVigencia() {
        return vigencia;
    }

    public Instant getRevocadoAt() {
        return revocadoAt;
    }
}
