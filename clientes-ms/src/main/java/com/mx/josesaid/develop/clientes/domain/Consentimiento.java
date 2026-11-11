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

/** Consentimiento de un contacto para una finalidad concreta. Se conserva aunque se revoque. */
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

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected Consentimiento() {
    }

    /** Crea un consentimiento otorgado, con su vigencia. */
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

    /** Devuelve el identificador. */
    public UUID getId() {
        return id;
    }

    /** Devuelve el id del contacto. */
    public UUID getContactoId() {
        return contactoId;
    }

    /** Devuelve la finalidad. */
    public String getFinalidad() {
        return finalidad;
    }

    /** Devuelve la fecha en que se otorgó. */
    public Instant getFechaOtorgado() {
        return fechaOtorgado;
    }

    /** Devuelve el medio por el que se otorgó. */
    public MedioConsentimiento getMedio() {
        return medio;
    }

    /** Devuelve la fecha hasta la que es vigente. */
    public LocalDate getVigencia() {
        return vigencia;
    }

    /** Devuelve el momento de la revocación, o nulo si sigue vigente. */
    public Instant getRevocadoAt() {
        return revocadoAt;
    }
}
