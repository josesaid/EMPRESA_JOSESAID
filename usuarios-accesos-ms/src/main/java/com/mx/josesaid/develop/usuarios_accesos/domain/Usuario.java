package com.mx.josesaid.develop.usuarios_accesos.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Agregado raíz de identidad.
 *
 * <p>Contiene las reglas de acceso del negocio: bloqueo por intentos fallidos,
 * baja definitiva y asignación de roles con motivo. No depende de Spring, JPA ni Kafka.
 */
public class Usuario {

    /** Intentos fallidos consecutivos antes de bloquear la cuenta (propuesta, por validar). */
    public static final int MAX_INTENTOS_FALLIDOS = 5;

    /** Duración del bloqueo temporal (propuesta, por validar). */
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final UUID id;
    private String correo;
    private String nombreCompleto;
    private String passwordHash;
    private final Set<Rol> roles;
    private EstatusUsuario estatus;
    private int intentosFallidos;
    private Instant bloqueadoHasta;
    private String area;

    private Usuario(UUID id, String correo, String nombreCompleto, String passwordHash,
                    Set<Rol> roles, EstatusUsuario estatus, int intentosFallidos, Instant bloqueadoHasta) {
        this.id = id;
        this.correo = correo;
        this.nombreCompleto = nombreCompleto;
        this.passwordHash = passwordHash;
        this.roles = roles;
        this.estatus = estatus;
        this.intentosFallidos = intentosFallidos;
        this.bloqueadoHasta = bloqueadoHasta;
    }

    /** Crea un usuario activo nuevo. El hash de contraseña llega ya calculado por la capa de aplicación. */
    public static Usuario crear(String correo, String nombreCompleto, String passwordHash, Collection<Rol> roles) {
        return new Usuario(
                UUID.randomUUID(),
                normalizarCorreo(correo),
                validarNombre(nombreCompleto),
                requerido(passwordHash, "La contraseña es obligatoria"),
                validarRoles(roles),
                EstatusUsuario.ACTIVO,
                0,
                null);
    }

    /** Reconstruye un usuario desde la persistencia, sin volver a validar las reglas de creación. */
    public static Usuario reconstruir(UUID id, String correo, String nombreCompleto, String passwordHash,
                                      Collection<Rol> roles, EstatusUsuario estatus,
                                      int intentosFallidos, Instant bloqueadoHasta) {
        return new Usuario(id, correo, nombreCompleto, passwordHash,
                EnumSet.copyOf(roles.isEmpty() ? EnumSet.noneOf(Rol.class) : roles),
                estatus, intentosFallidos, bloqueadoHasta);
    }

    /** Normaliza el correo a minúsculas sin espacios y valida su forma básica. */
    public static String normalizarCorreo(String correo) {
        String limpio = requerido(correo, "El correo es obligatorio").trim().toLowerCase();
        if (!limpio.contains("@")) {
            throw new ReglaDeNegocioException("El correo no tiene un formato válido");
        }
        return limpio;
    }

    /**
     * Verifica que la cuenta pueda intentar iniciar sesión.
     * Una cuenta bloqueada cuyo plazo ya venció se desbloquea aquí.
     */
    public void verificarAcceso(Instant ahora) {
        if (estatus == EstatusUsuario.BAJA) {
            // Se responde igual que una credencial incorrecta para no revelar la baja
            throw new CredencialesInvalidasException();
        }
        if (estatus == EstatusUsuario.BLOQUEADO) {
            if (bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta)) {
                throw new BloqueoException("La cuenta está bloqueada temporalmente. Intenta más tarde.");
            }
            estatus = EstatusUsuario.ACTIVO;
            intentosFallidos = 0;
            bloqueadoHasta = null;
        }
    }

    /**
     * Registra una contraseña incorrecta.
     *
     * @return true solo cuando este intento provoca el bloqueo de la cuenta
     */
    public boolean registrarIntentoFallido(Instant ahora) {
        intentosFallidos++;
        if (intentosFallidos >= MAX_INTENTOS_FALLIDOS) {
            estatus = EstatusUsuario.BLOQUEADO;
            bloqueadoHasta = ahora.plus(DURACION_BLOQUEO);
            return true;
        }
        return false;
    }

    /** Reinicia los contadores tras un inicio de sesión exitoso. */
    public void registrarLoginExitoso() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        estatus = EstatusUsuario.ACTIVO;
    }

    /**
     * Reemplaza los roles del usuario. El motivo es obligatorio para auditoría.
     */
    public void asignarRoles(Collection<Rol> nuevos, String motivo) {
        if (estatus == EstatusUsuario.BAJA) {
            throw new ReglaDeNegocioException("No se pueden asignar roles a un usuario dado de baja");
        }
        requerido(motivo, "El motivo del cambio de roles es obligatorio");
        Set<Rol> validados = validarRoles(nuevos);
        if (validados.contains(Rol.JEFE_AREA) && (area == null || area.isBlank())) {
            throw new ReglaDeNegocioException("Asigne un área al usuario antes del rol JEFE_AREA");
        }
        roles.clear();
        roles.addAll(validados);
    }

    /**
     * Asigna el área del usuario. Un JEFE_AREA solo consulta indicadores de esta área.
     * El motivo es obligatorio para auditoría.
     */
    public void asignarArea(String nuevaArea, String motivo) {
        if (estatus == EstatusUsuario.BAJA) {
            throw new ReglaDeNegocioException("No se puede asignar área a un usuario dado de baja");
        }
        area = requerido(nuevaArea, "El área es obligatoria");
        requerido(motivo, "El motivo del cambio de área es obligatorio");
    }

    /** Fija el área al reconstruir desde persistencia. */
    public Usuario conArea(String area) {
        this.area = area;
        return this;
    }

    /** Da de baja al usuario. Es definitivo: no se puede reactivar; se debe crear uno nuevo. */
    public void darDeBaja() {
        if (estatus == EstatusUsuario.BAJA) {
            throw new ReglaDeNegocioException("El usuario ya está dado de baja");
        }
        estatus = EstatusUsuario.BAJA;
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    private static Set<Rol> validarRoles(Collection<Rol> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new ReglaDeNegocioException("Debe asignar al menos un rol");
        }
        return EnumSet.copyOf(roles);
    }

    private static String validarNombre(String nombre) {
        return requerido(nombre, "El nombre completo es obligatorio").trim();
    }

    private static String requerido(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new ReglaDeNegocioException(mensaje);
        }
        return valor;
    }

    public UUID getId() {
        return id;
    }

    public String getCorreo() {
        return correo;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    /** Copia defensiva: el conjunto interno solo cambia mediante {@link #asignarRoles}. */
    public Set<Rol> getRoles() {
        return EnumSet.copyOf(roles.isEmpty() ? EnumSet.noneOf(Rol.class) : roles);
    }

    public String getArea() {
        return area;
    }

    public EstatusUsuario getEstatus() {
        return estatus;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public Instant getBloqueadoHasta() {
        return bloqueadoHasta;
    }
}
