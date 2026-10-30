package com.mx.josesaid.develop.usuarios_accesos.api;

import com.mx.josesaid.develop.usuarios_accesos.application.SesionService;
import com.mx.josesaid.develop.usuarios_accesos.application.UsuarioService;
import com.mx.josesaid.develop.usuarios_accesos.config.PropiedadesJwt;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.infrastructure.security.EmisorTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Inicio de sesión. Público: es la única forma de obtener un token. */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final UsuarioService servicio;
    private final EmisorTokens emisor;
    private final PropiedadesJwt propiedades;
    private final SesionService sesiones;

    public AuthController(UsuarioService servicio, EmisorTokens emisor, PropiedadesJwt propiedades,
                          SesionService sesiones) {
        this.servicio = servicio;
        this.emisor = emisor;
        this.propiedades = propiedades;
        this.sesiones = sesiones;
    }

    @PostMapping("/login")
    public TokenRespuesta login(@Valid @RequestBody LoginSolicitud solicitud) {
        Usuario usuario = servicio.autenticar(solicitud.correo(), solicitud.password());
        return respuesta(sesiones.iniciar(usuario));
    }

    /** Rota el refresh token y devuelve un access token y un refresh token nuevos. */
    @PostMapping("/refresh")
    public TokenRespuesta refrescar(@Valid @RequestBody RefrescoSolicitud solicitud) {
        return respuesta(sesiones.refrescar(solicitud.refreshToken()));
    }

    /** Sustituye la contraseña temporal. Público: el usuario aún no tiene token. */
    @PostMapping("/cambiar-password-inicial")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarPasswordInicial(@Valid @RequestBody CambioPasswordSolicitud solicitud) {
        servicio.cambiarPasswordTemporal(solicitud.correo(), solicitud.passwordActual(), solicitud.passwordNueva());
    }

    /** Cierra la sesión del refresh token dado. Público: basta con tener el token. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrar(@Valid @RequestBody RefrescoSolicitud solicitud) {
        sesiones.cerrar(solicitud.refreshToken());
    }

    /** Cierra todas las sesiones del usuario autenticado. */
    @PostMapping("/logout-todas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cerrarTodas(@AuthenticationPrincipal Jwt jwt) {
        sesiones.cerrarTodas(UUID.fromString(jwt.getSubject()));
    }

    private TokenRespuesta respuesta(SesionService.Par par) {
        return new TokenRespuesta(par.accessToken(), "Bearer", propiedades.duracion().toSeconds(), par.refreshToken());
    }

    public record LoginSolicitud(@NotBlank @Email String correo, @NotBlank String password) {
    }

    public record RefrescoSolicitud(@NotBlank String refreshToken) {
    }

    public record CambioPasswordSolicitud(
            @NotBlank @Email String correo,
            @NotBlank String passwordActual,
            @NotBlank @Size(min = 12, max = 100) String passwordNueva) {
    }

    public record TokenRespuesta(String accessToken, String tokenType, long expiresIn, String refreshToken) {
    }
}
