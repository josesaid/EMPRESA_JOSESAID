package com.mx.josesaid.develop.ui.web;

import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.mx.josesaid.develop.ui.nexus.NexusCliente;
import com.mx.josesaid.develop.ui.nexus.SugerenciaDto;

/** Proponer una póliza, confirmarla con el código o cancelarla. Nexus valida el rol en el backend. */
@Controller
public class AccionesController {

    private static final String VISTA = "acciones/polizas-nueva";

    private final NexusCliente nexus;

    public AccionesController(NexusCliente nexus) {
        this.nexus = nexus;
    }

    @GetMapping("/polizas/nueva")
    public String formulario(Authentication usuario, Model modelo) {
        preparar(usuario, modelo);
        return VISTA;
    }

    @PostMapping("/polizas/nueva")
    public String sugerir(@RequestParam String descripcion, Authentication usuario, Model modelo) {
        preparar(usuario, modelo);
        modelo.addAttribute("descripcion", descripcion);
        if (!esContador(usuario)) {
            return VISTA;
        }
        try {
            SugerenciaDto sugerencia = nexus.sugerirPoliza(descripcion.trim());
            modelo.addAttribute("sugerencia", sugerencia);
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeSugerir(estado(e)));
        } catch (RestClientException e) {
            modelo.addAttribute("error", MENSAJE_NO_DISPONIBLE);
        }
        return VISTA;
    }

    @PostMapping("/acciones/{id}/confirmacion")
    public String confirmar(@PathVariable UUID id, @RequestParam String codigo, Authentication usuario, Model modelo) {
        preparar(usuario, modelo);
        try {
            nexus.confirmar(id, codigo.trim());
            modelo.addAttribute("mensaje", "Póliza registrada.");
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeConfirmar(estado(e)));
        } catch (RestClientException e) {
            modelo.addAttribute("error", MENSAJE_NO_DISPONIBLE);
        }
        return VISTA;
    }

    @PostMapping("/acciones/{id}/cancelacion")
    public String cancelar(@PathVariable UUID id, Authentication usuario, Model modelo) {
        preparar(usuario, modelo);
        try {
            nexus.cancelar(id);
            modelo.addAttribute("mensaje", "Propuesta cancelada. Nada se registró.");
        } catch (HttpClientErrorException e) {
            modelo.addAttribute("error", mensajeCancelar(estado(e)));
        } catch (RestClientException e) {
            modelo.addAttribute("error", MENSAJE_NO_DISPONIBLE);
        }
        return VISTA;
    }

    private static final String MENSAJE_NO_DISPONIBLE = "Nexus no está disponible. Intenta más tarde.";

    /** Decide por código HTTP: las subclases de HttpClientErrorException no cubren todos los estatus. */
    private static int estado(HttpClientErrorException e) {
        return e.getStatusCode().value();
    }

    private static String mensajeSugerir(int estado) {
        return switch (estado) {
            case 403 -> "Tu rol no puede proponer pólizas.";
            case 422 -> "No pude interpretar la operación. Describe cuentas y montos con más detalle.";
            default -> "Revisa la descripción e intenta de nuevo.";
        };
    }

    private static String mensajeConfirmar(int estado) {
        return switch (estado) {
            case 422 -> "El código no coincide. La propuesta se canceló; vuelve a proponer la póliza.";
            case 409 -> "La propuesta ya no está pendiente.";
            case 404 -> "La propuesta no existe.";
            case 403 -> "Tu rol no puede confirmar esta propuesta.";
            default -> "Revisa el código de 8 dígitos e intenta de nuevo.";
        };
    }

    private static String mensajeCancelar(int estado) {
        return switch (estado) {
            case 409 -> "La propuesta ya no está pendiente.";
            case 404 -> "La propuesta no existe.";
            default -> "No se pudo cancelar la propuesta.";
        };
    }

    private void preparar(Authentication usuario, Model modelo) {
        modelo.addAttribute("correo", usuario.getName());
        modelo.addAttribute("titulo", "Proponer póliza");
        boolean contador = esContador(usuario);
        modelo.addAttribute("puedeProponer", contador);
        if (!contador) {
            modelo.addAttribute("error", "Solo el rol CONTADOR puede proponer pólizas.");
        }
    }

    private static boolean esContador(Authentication usuario) {
        return usuario.getAuthorities().stream().anyMatch(a -> "ROLE_CONTADOR".equals(a.getAuthority()));
    }
}
