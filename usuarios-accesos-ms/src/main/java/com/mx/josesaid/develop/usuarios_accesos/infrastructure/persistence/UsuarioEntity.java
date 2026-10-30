package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import com.mx.josesaid.develop.usuarios_accesos.domain.EstatusUsuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Modelo de persistencia de usuarios. Separado del agregado de dominio a propósito. */
@Entity
@Table(name = "usuarios")
public class UsuarioEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String correo;

    @Column(name = "nombre_completo", nullable = false, length = 200)
    private String nombreCompleto;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstatusUsuario estatus;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    /** Control de concurrencia optimista. Lo gestiona JPA. */
    @Version
    private Long version;

    @Column(name = "creado_utc", nullable = false, updatable = false)
    private Instant creadoUtc;

    @Column(name = "actualizado_utc", nullable = false)
    private Instant actualizadoUtc;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_roles", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", length = 30)
    private Set<Rol> roles = new HashSet<>();

    protected UsuarioEntity() {
        // requerido por JPA
    }

    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        creadoUtc = ahora;
        actualizadoUtc = ahora;
    }

    @PreUpdate
    void alActualizar() {
        actualizadoUtc = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public EstatusUsuario getEstatus() { return estatus; }
    public void setEstatus(EstatusUsuario estatus) { this.estatus = estatus; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public Long getVersion() { return version; }
    public Set<Rol> getRoles() { return roles; }
}
