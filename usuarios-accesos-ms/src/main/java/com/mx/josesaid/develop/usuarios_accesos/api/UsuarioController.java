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

    public UsuarioController(UsuarioService servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioRespuesta crear(@Valid @RequestBody CrearUsuarioSolicitud solicitud) {
        Usuario usuario = servicio.crearConPasswordTemporal(solicitud.correo(), solicitud.nombreCompleto(),
                solicitud.password(), solicitud.roles());
        return UsuarioRespuesta.de(usuario);
    }

    @GetMapping("/{id}")
    public UsuarioRespuesta obtener(@PathVariable UUID id) {
        return UsuarioRespuesta.de(servicio.obtener(id));
    }

    @PutMapping("/{id}/roles")
    public UsuarioRespuesta asignarRoles(@PathVariable UUID id,
                                         @Valid @RequestBody AsignarRolesSolicitud solicitud) {
        return UsuarioRespuesta.de(servicio.asignarRoles(id, solicitud.roles(), solicitud.motivo()));
    }

    @PutMapping("/{id}/area")
    public UsuarioRespuesta asignarArea(@PathVariable UUID id,
                                        @Valid @RequestBody AsignarAreaSolicitud solicitud) {
        return UsuarioRespuesta.de(servicio.asignarArea(id, solicitud.area(), solicitud.motivo()));
    }

    @PostMapping("/{id}/baja")
    public UsuarioRespuesta darDeBaja(@PathVariable UUID id) {
        return UsuarioRespuesta.de(servicio.darDeBaja(id));
    }

    public record CrearUsuarioSolicitud(
            @NotBlank @Email String correo,
            @NotBlank String nombreCompleto,
            @NotBlank String password,
            @NotEmpty Set<Rol> roles) {
    }

    public record AsignarAreaSolicitud(
            @NotBlank @Size(max = 40) String area,
            @NotBlank String motivo) {
    }

    public record AsignarRolesSolicitud(
            @NotNull @NotEmpty Set<Rol> roles,
            @NotBlank String motivo) {
    }

    /** Respuesta con datos personales enmascarados. */
    public record UsuarioRespuesta(UUID id, String correo, String nombreCompleto, String estatus, Set<Rol> roles,
                                   String area) {
        static UsuarioRespuesta de(Usuario u) {
            return new UsuarioRespuesta(u.getId(), Enmascarado.correo(u.getCorreo()),
                    Enmascarado.nombre(u.getNombreCompleto()), u.getEstatus().name(), u.getRoles(),
                    u.getArea());
        }
    }
}
