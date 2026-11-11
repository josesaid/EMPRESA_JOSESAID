package com.mx.josesaid.develop.usuarios_accesos.infrastructure.persistence;

import com.mx.josesaid.develop.usuarios_accesos.domain.EstatusUsuario;
import com.mx.josesaid.develop.usuarios_accesos.infrastructure.crypto.CampoCifradoConverter;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Convert;
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

    /** Cifrado AES-256-GCM. La unicidad la garantiza {@code correo_indice}. */
    @Convert(converter = CampoCifradoConverter.class)
    @Column(nullable = false, length = 400)
    private String correo;

    @Column(name = "correo_indice", length = 64)
    private String correoIndice;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "nombre_completo", nullable = false, length = 400)
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

    @Column(length = 40)
    private String area;

    @Column(name = "debe_cambiar_password", nullable = false)
    private boolean debeCambiarPassword;

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

    /** Constructor exigido por JPA. No se usa en el código de la aplicación. */
    protected UsuarioEntity() {
        // requerido por JPA
    }

    /** Se ejecuta antes de insertar la fila: fija la fecha de creación. */
    @PrePersist
    void alCrear() {
        Instant ahora = Instant.now();
        creadoUtc = ahora;
        actualizadoUtc = ahora;
    }

    /** Se ejecuta antes de actualizar la fila: fija la fecha de modificación. */
    @PreUpdate
    void alActualizar() {
        actualizadoUtc = Instant.now();
    }

    /** Devuelve el identificador. */
    public UUID getId() { return id; }
    /** Asigna el identificador. */
    public void setId(UUID id) { this.id = id; }
    /** Devuelve el correo tal como se guardó (cifrado). */
    public String getCorreo() { return correo; }
    /** Devuelve el índice de búsqueda del correo. */
    public String getCorreoIndice() { return correoIndice; }
    /** Asigna el índice de búsqueda del correo. */
    public void setCorreoIndice(String correoIndice) { this.correoIndice = correoIndice; }
    /** Asigna el correo, cifrado al guardarse. */
    public void setCorreo(String correo) { this.correo = correo; }
    /** Devuelve el nombre completo tal como se guardó (cifrado). */
    public String getNombreCompleto() { return nombreCompleto; }
    /** Asigna el nombre completo, cifrado al guardarse. */
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    /** Devuelve el hash de la contraseña. */
    public String getPasswordHash() { return passwordHash; }
    /** Asigna el hash de la contraseña. */
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    /** Devuelve el estatus del usuario. */
    public EstatusUsuario getEstatus() { return estatus; }
    /** Asigna el estatus del usuario. */
    public void setEstatus(EstatusUsuario estatus) { this.estatus = estatus; }
    /** Devuelve los intentos fallidos consecutivos. */
    public int getIntentosFallidos() { return intentosFallidos; }
    /** Asigna los intentos fallidos consecutivos. */
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    /** Devuelve hasta cuándo está bloqueada la cuenta. */
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    /** Asigna hasta cuándo está bloqueada la cuenta. */
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    /** Devuelve el área del usuario. */
    public String getArea() { return area; }
    /** Asigna el área del usuario. */
    public void setArea(String area) { this.area = area; }
    /** Indica si la contraseña es temporal. */
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }
    /** Asigna el indicador de contraseña temporal. */
    public void setDebeCambiarPassword(boolean valor) { this.debeCambiarPassword = valor; }
    /** Devuelve la versión para control de concurrencia optimista. */
    public Long getVersion() { return version; }
    /** Devuelve los roles del usuario. */
    public Set<Rol> getRoles() { return roles; }
}
