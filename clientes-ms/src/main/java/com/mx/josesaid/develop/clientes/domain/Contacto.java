package com.mx.josesaid.develop.clientes.domain;

import com.mx.josesaid.develop.clientes.infrastructure.crypto.CampoCifradoConverter;
import com.mx.josesaid.develop.clientes.infrastructure.crypto.IndiceHash;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "contacto")
public class Contacto {

    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "nombre_cifrado", nullable = false)
    private String nombre;

    @Convert(converter = CampoCifradoConverter.class)
    @Column(name = "correo_cifrado", nullable = false)
    private String correo;

    @Column(name = "correo_hash", nullable = false, length = 64)
    private String correoHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 20)
    private RolContacto rol;

    @Column(name = "principal", nullable = false)
    private boolean principal;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Contacto() {
    }

    public static Contacto nuevo(UUID id, UUID clienteId, String nombre, String correo, RolContacto rol,
                                 boolean principal, Instant ahora) {
        Contacto c = new Contacto();
        c.id = id;
        c.clienteId = clienteId;
        c.nombre = nombre.trim();
        c.correo = correo.trim();
        c.correoHash = IndiceHash.correo(correo);
        c.rol = rol;
        c.principal = principal;
        c.createdAt = ahora;
        c.updatedAt = ahora;
        return c;
    }

    public void desmarcarPrincipal(Instant ahora) {
        this.principal = false;
        this.updatedAt = ahora;
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public RolContacto getRol() {
        return rol;
    }

    public boolean isPrincipal() {
        return principal;
    }
}
