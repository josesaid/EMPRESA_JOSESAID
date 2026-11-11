package com.mx.josesaid.develop.ui.web;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.mx.josesaid.develop.ui.nexus.DatosAltaDto;
import com.mx.josesaid.develop.ui.nexus.NexusCliente;
import com.mx.josesaid.develop.ui.usuarios.UsuariosCliente;

/**
 * Completa un alta preparada por Nexus. El administrador escribe la contraseña aquí; el asistente nunca la ve.
 * Orden: verificar código en Nexus, crear usuario en usuarios-accesos, marcar el alta como completada.
 */
@Controller
@RequestMapping("/usuarios/altas")
public class UsuariosAltaController {

    private static final String VISTA = "usuarios/alta";
    private static final String MENSAJE_NO_DISPONIBLE = "Nexus o usuarios no están disponibles. Intenta más tarde.";

    private final NexusCliente nexus;
    private final UsuariosCliente usuarios;

    /** Recibe el cliente de Nexus y el de usuarios. */
    public UsuariosAltaController(NexusCliente nexus, UsuariosCliente usuarios) {
        this.nexus = nexus;
        this.usuarios = usuarios;
    }

    /** Muestra el formulario para completar el alta de un usuario con su código de verificación. */
    @GetMapping("/{id}")
    public String formulario(@PathVariable UUID id, Authentication usuario, Model modelo) {
        modelo.addAttribute("id", id);
        if (!esAdmin(usuario)) {
            modelo.addAttribute("error", "Solo el rol ADMIN puede dar de alta usuarios.");
            return VISTA;
        }
        try {
            modelo.addAttribute("datos", nexus.datosAlta(id));
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeNexus(e.getStatusCode().value()));
        } catch (RestClientException e) {
            modelo.addAttribute("error", MENSAJE_NO_DISPONIBLE);
        }
        return VISTA;
    }

    /** Completa el alta del usuario con su código y la contraseña inicial. Si falla, vuelve al formulario con el motivo. */
    @PostMapping("/{id}")
    public String completar(@PathVariable UUID id,
                            @RequestParam String codigo,
                            @RequestParam String password,
                            Authentication usuario,
                            Model modelo) {
        modelo.addAttribute("id", id);
        if (!esAdmin(usuario)) {
            modelo.addAttribute("error", "Solo el rol ADMIN puede dar de alta usuarios.");
            return VISTA;
        }
        String codigoLimpio = codigo.trim();
        DatosAltaDto datos;
        try {
            datos = nexus.datosAlta(id);
            modelo.addAttribute("datos", datos);
            nexus.verificarAlta(id, codigoLimpio);
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeNexus(e.getStatusCode().value()));
            return VISTA;
        } catch (RestClientException e) {
            modelo.addAttribute("error", MENSAJE_NO_DISPONIBLE);
            return VISTA;
        }
        try {
            usuarios.crear(datos, password);
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeUsuarios(e.getStatusCode().value()));
            return VISTA;
        } catch (RestClientException e) {
            modelo.addAttribute("error", MENSAJE_NO_DISPONIBLE);
            return VISTA;
        }
        try {
            nexus.completarAlta(id, codigoLimpio);
        } catch (RestClientException e) {
            // El usuario ya existe: no se reintenta la creación. Se avisa para revisarlo.
            modelo.addAttribute("error", "El usuario " + datos.correo() + " se creó, pero no pude marcar el alta "
                    + "como completada. No la repitas; revisa el alta en Nexus.");
            return VISTA;
        }
        modelo.addAttribute("mensaje", "Usuario " + datos.correo() + " creado. Comparte la contraseña temporal "
                + "por un canal seguro.");
        return VISTA;
    }

    /** Traduce el estado de Nexus en un mensaje de verificación. */
    private static String mensajeNexus(int estado) {
        return switch (estado) {
            case 422 -> "El código no coincide. El alta se canceló; pide a Nexus que la prepare de nuevo.";
            case 409 -> "El alta ya no está pendiente.";
            case 404 -> "El alta no existe.";
            case 403 -> "Tu rol no puede completar altas.";
            default -> "Revisa el código de 8 dígitos e intenta de nuevo.";
        };
    }

    /** Traduce el estado de usuarios en un mensaje de alta. */
    private static String mensajeUsuarios(int estado) {
        return switch (estado) {
            case 409 -> "Ya existe un usuario con ese correo. No se creó nada; el alta sigue pendiente.";
            case 400 -> "La contraseña o los datos no cumplen las reglas. Revisa e intenta de nuevo.";
            case 403 -> "Tu rol no puede crear usuarios.";
            default -> "No se pudo crear el usuario. El alta sigue pendiente; intenta de nuevo.";
        };
    }

    /** Indica si el usuario tiene el rol de administrador. */
    private static boolean esAdmin(Authentication usuario) {
        return usuario.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
