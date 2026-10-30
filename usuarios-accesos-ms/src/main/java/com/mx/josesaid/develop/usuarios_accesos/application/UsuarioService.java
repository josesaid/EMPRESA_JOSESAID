package com.mx.josesaid.develop.usuarios_accesos.application;

import com.mx.josesaid.develop.usuarios_accesos.domain.CambioPasswordRequeridoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.ReglaDeNegocioException;
import com.mx.josesaid.develop.usuarios_accesos.domain.CredencialesInvalidasException;
import com.mx.josesaid.develop.usuarios_accesos.domain.ConflictoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.RecursoNoEncontradoException;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.domain.UsuarioRepositorio;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de identidad: alta, autenticación, asignación de roles y baja.
 *
 * <p>Las transacciones son de la capa de aplicación. El intento fallido se guarda aunque
 * la respuesta sea 401, por eso {@code autenticar} no revierte ante credenciales incorrectas.
 */
@Service
public class UsuarioService {

    public static final String EVENTO_USUARIO_BLOQUEADO = "usuario-bloqueado";
    public static final String EVENTO_ROL_ASIGNADO = "rol-asignado";
    public static final String EVENTO_AREA_ASIGNADA = "area-asignada";

    private final UsuarioRepositorio repositorio;
    private final PasswordEncoder passwordEncoder;
    private final PublicadorEventos publicador;
    private final Clock reloj;

    public UsuarioService(UsuarioRepositorio repositorio, PasswordEncoder passwordEncoder,
                          PublicadorEventos publicador, Clock reloj) {
        this.repositorio = repositorio;
        this.passwordEncoder = passwordEncoder;
        this.publicador = publicador;
        this.reloj = reloj;
    }

    @Transactional
    public Usuario crear(String correo, String nombreCompleto, String passwordPlano, Set<Rol> roles) {
        String correoNormalizado = Usuario.normalizarCorreo(correo);
        if (repositorio.existePorCorreo(correoNormalizado)) {
            throw new ConflictoException("Ya existe un usuario con ese correo");
        }
        if (roles != null && roles.contains(Rol.JEFE_AREA)) {
            throw new ReglaDeNegocioException("Cree el usuario, asígnele un área y después el rol JEFE_AREA");
        }
        String hash = passwordEncoder.encode(passwordPlano == null ? "" : passwordPlano);
        return repositorio.guardar(Usuario.crear(correoNormalizado, nombreCompleto, hash, roles));
    }

    /** Da de alta una cuenta con contraseña temporal: debe cambiarla antes de iniciar sesión. */
    @Transactional
    public Usuario crearConPasswordTemporal(String correo, String nombreCompleto, String passwordPlano, Set<Rol> roles) {
        Usuario creado = crear(correo, nombreCompleto, passwordPlano, roles);
        creado.marcarPasswordTemporal();
        return repositorio.guardar(creado);
    }

    /**
     * Valida credenciales. Devuelve el usuario si son correctas.
     *
     * <p>{@code noRollbackFor} conserva el contador de intentos fallidos cuando las credenciales son incorrectas.
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public Usuario autenticar(String correo, String passwordPlano) {
        Instant ahora = Instant.now(reloj);
        Usuario usuario = repositorio.buscarPorCorreo(Usuario.normalizarCorreo(correo))
                .orElseThrow(CredencialesInvalidasException::new);

        usuario.verificarAcceso(ahora);

        if (!passwordEncoder.matches(passwordPlano == null ? "" : passwordPlano, usuario.getPasswordHash())) {
            boolean bloqueadoAhora = usuario.registrarIntentoFallido(ahora);
            repositorio.guardar(usuario);
            if (bloqueadoAhora) {
                publicador.publicar(new EventoDominio(
                        EVENTO_USUARIO_BLOQUEADO,
                        usuario.getId().toString(),
                        Map.of("usuarioId", usuario.getId().toString(),
                               "intentosFallidos", usuario.getIntentosFallidos())));
            }
            throw new CredencialesInvalidasException();
        }

        if (usuario.debeCambiarPassword()) {
            throw new CambioPasswordRequeridoException();
        }
        usuario.registrarLoginExitoso();
        return repositorio.guardar(usuario);
    }

    /**
     * Sustituye la contraseña temporal por una definitiva. Exige la contraseña temporal actual.
     * No emite tokens: después de cambiarla, el usuario inicia sesión normalmente.
     */
    @Transactional(noRollbackFor = CredencialesInvalidasException.class)
    public void cambiarPasswordTemporal(String correo, String passwordActual, String passwordNueva) {
        Instant ahora = Instant.now(reloj);
        Usuario usuario = repositorio.buscarPorCorreo(Usuario.normalizarCorreo(correo))
                .orElseThrow(CredencialesInvalidasException::new);
        usuario.verificarAcceso(ahora);
        if (!passwordEncoder.matches(passwordActual == null ? "" : passwordActual, usuario.getPasswordHash())) {
            boolean bloqueadoAhora = usuario.registrarIntentoFallido(ahora);
            repositorio.guardar(usuario);
            if (bloqueadoAhora) {
                publicador.publicar(new EventoDominio(
                        EVENTO_USUARIO_BLOQUEADO,
                        usuario.getId().toString(),
                        Map.of("usuarioId", usuario.getId().toString(),
                               "intentosFallidos", usuario.getIntentosFallidos())));
            }
            throw new CredencialesInvalidasException();
        }
        if (passwordEncoder.matches(passwordNueva, usuario.getPasswordHash())) {
            throw new ReglaDeNegocioException("La nueva contraseña debe ser distinta a la temporal");
        }
        usuario.cambiarPasswordTemporal(passwordEncoder.encode(passwordNueva));
        repositorio.guardar(usuario);
    }

    @Transactional
    public Usuario asignarRoles(UUID id, Set<Rol> nuevosRoles, String motivo) {
        Usuario usuario = obtener(id);
        List<String> anteriores = nombres(usuario.getRoles());
        usuario.asignarRoles(nuevosRoles, motivo);
        Usuario guardado = repositorio.guardar(usuario);
        publicador.publicar(new EventoDominio(
                EVENTO_ROL_ASIGNADO,
                id.toString(),
                Map.of("usuarioId", id.toString(),
                       "rolesAnteriores", anteriores,
                       "rolesNuevos", nombres(guardado.getRoles()),
                       "motivo", motivo)));
        return guardado;
    }

    @Transactional
    public Usuario asignarArea(UUID id, String area, String motivo) {
        Usuario usuario = obtener(id);
        String anterior = usuario.getArea() == null ? "" : usuario.getArea();
        usuario.asignarArea(area, motivo);
        Usuario guardado = repositorio.guardar(usuario);
        publicador.publicar(new EventoDominio(
                EVENTO_AREA_ASIGNADA,
                id.toString(),
                Map.of("usuarioId", id.toString(),
                       "areaAnterior", anterior,
                       "areaNueva", guardado.getArea(),
                       "motivo", motivo)));
        return guardado;
    }

    @Transactional
    public Usuario darDeBaja(UUID id) {
        Usuario usuario = obtener(id);
        usuario.darDeBaja();
        return repositorio.guardar(usuario);
    }

    @Transactional(readOnly = true)
    public Usuario obtener(UUID id) {
        return repositorio.buscarPorId(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado"));
    }

    private static List<String> nombres(Collection<Rol> roles) {
        return roles.stream().map(Enum::name).sorted().collect(Collectors.toList());
    }
}
