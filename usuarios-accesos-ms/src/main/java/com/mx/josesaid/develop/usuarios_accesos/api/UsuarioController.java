package com.mx.josesaid.develop.usuarios_accesos.api;

import com.mx.josesaid.develop.usuarios_accesos.application.UsuarioService;
import com.mx.josesaid.develop.usuarios_accesos.domain.Rol;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Administración de usuarios. Solo el rol ADMIN puede usar estos endpoints. */
@RestController
@RequestMapping("/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService servicio;

    /** Recibe el servicio de usuarios. */
    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    /** Da de alta un usuario con su contraseña temporal. Debe cambiarla antes de su primer inicio de sesión. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta crear(@Valid @RequestBody CrearUsuarioSolicitud solicitud) {
        Usuario usuario = servicio.crearConPasswordTemporal(solicitud.correo(), solicitud.nombreCompleto(),
                solicitud.password(), solicitud.roles());
        return UsuarioRespuesta.de(usuario);
    }

    /** Lista los usuarios más recientes. */
    @GetMapping
    public List<UsuarioRespuesta> listar() {
        return servicio.listarRecientes().stream().map(UsuarioRespuesta::de).toList();
    }

    /** Devuelve un usuario por id. */
    @GetMapping("/{id}")
    public UsuarioRespuesta obtener(@PathVariable UUID id) {
        return UsuarioRespuesta.de(servicio.obtener(id));
    }

    /** Reemplaza los roles de un usuario. Exige un motivo para la auditoría. */
    @PutMapping("/{id}/roles")
    public UsuarioRespuesta asignarRoles(@PathVariable UUID id,
                                         @Valid @RequestBody AsignarRolesSolicitud solicitud) {
        return UsuarioRespuesta.de(servicio.asignarRoles(id, solicitud.roles(), solicitud.motivo()));
    }

    /** Asigna el área de un usuario. Exige un motivo para la auditoría. */
    @PutMapping("/{id}/area")
    public UsuarioRespuesta asignarArea(@PathVariable UUID id,
                                        @Valid @RequestBody AsignarAreaSolicitud solicitud) {
        return UsuarioRespuesta.de(servicio.asignarArea(id, solicitud.area(), solicitud.motivo()));
    }

    /** Da de baja al usuario de forma definitiva. */
    @PostMapping("/{id}/baja")
    public UsuarioRespuesta darDeBaja(@PathVariable UUID id) {
        return UsuarioRespuesta.de(servicio.darDeBaja(id));
    }

    /** Datos para crear un usuario: correo, nombre completo, contraseña temporal y roles. */
    public record CrearUsuarioSolicitud(
            @NotBlank @Email String correo,
            @NotBlank String nombreCompleto,
            @NotBlank String password,
            @NotEmpty Set<Rol> roles) {
    }

    /** Área a asignar y motivo del cambio. */
    public record AsignarAreaSolicitud(
            @NotBlank @Size(max = 40) String area,
            @NotBlank String motivo) {
    }

    /** Roles nuevos y motivo del cambio. */
    public record AsignarRolesSolicitud(
            @NotNull @NotEmpty Set<Rol> roles,
            @NotBlank String motivo) {
    }

    /** Respuesta con datos personales enmascarados. */
    public record UsuarioRespuesta(UUID id, String correo, String nombreCompleto, String estatus, Set<Rol> roles,
                                   String area) {
        /** Convierte un usuario en su respuesta. */
        static UsuarioRespuesta de(Usuario u) {
            return new UsuarioRespuesta(u.getId(), Enmascarado.correo(u.getCorreo()),
                    Enmascarado.nombre(u.getNombreCompleto()), u.getEstatus().name(), u.getRoles(),
                    u.getArea());
        }
    }
}
