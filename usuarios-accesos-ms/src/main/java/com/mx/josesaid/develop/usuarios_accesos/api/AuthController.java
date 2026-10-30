package com.mx.josesaid.develop.usuarios_accesos.api;

import com.mx.josesaid.develop.usuarios_accesos.application.UsuarioService;
import com.mx.josesaid.develop.usuarios_accesos.config.PropiedadesJwt;
import com.mx.josesaid.develop.usuarios_accesos.domain.Usuario;
import com.mx.josesaid.develop.usuarios_accesos.infrastructure.security.EmisorTokens;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
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

    public AuthController(UsuarioService servicio, EmisorTokens emisor, PropiedadesJwt propiedades) {
        this.servicio = servicio;
        this.emisor = emisor;
        this.propiedades = propiedades;
    }

    @PostMapping("/login")
    public TokenRespuesta login(@Valid @RequestBody LoginSolicitud solicitud) {
        Usuario usuario = servicio.autenticar(solicitud.correo(), solicitud.password());
        return new TokenRespuesta(emisor.emitir(usuario), "Bearer", propiedades.duracion().toSeconds());
    }

    public record LoginSolicitud(@NotBlank @Email String correo, @NotBlank String password) {
    }

    public record TokenRespuesta(String accessToken, String tokenType, long expiresIn) {
    }
}
