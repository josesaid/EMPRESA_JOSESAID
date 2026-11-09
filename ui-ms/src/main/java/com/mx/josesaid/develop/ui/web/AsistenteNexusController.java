package com.mx.josesaid.develop.ui.web;

import com.mx.josesaid.develop.ui.nexus.NexusCliente;
import jakarta.servlet.http.HttpSession;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Pantalla "Asistente Nexus": chat libre con nexus-ms. El historial vive solo en la sesión actual. */
@Controller
@RequestMapping("/asistente")
public class AsistenteNexusController {

    static final String CLAVE_SESION = "historialAsistente";
    static final int MAX_MENSAJES = 50;
    static final int MAX_PREGUNTA = 2000;

    /** Un turno del chat: quién escribió y qué dijo. */
    public record Mensaje(String autor, String texto) implements Serializable {
    }

    private final NexusCliente nexus;

    public AsistenteNexusController(NexusCliente nexus) {
        this.nexus = nexus;
    }

    @GetMapping
    public String pantalla(HttpSession sesion, Model modelo) {
        modelo.addAttribute("titulo", "Asistente Nexus");
        modelo.addAttribute("seccion", "asistente");
        modelo.addAttribute("mensajes", historial(sesion));
        return "asistente/nexus";
    }

    @PostMapping
    public String preguntar(@RequestParam(required = false) String pregunta, HttpSession sesion,
                            Authentication usuario, RedirectAttributes redireccion) {
        String texto = pregunta == null ? "" : pregunta.trim();
        if (texto.isEmpty()) {
            redireccion.addFlashAttribute("error", "Escribe una pregunta antes de enviar.");
            return "redirect:/asistente";
        }
        if (texto.length() > MAX_PREGUNTA) {
            redireccion.addFlashAttribute("error", "La pregunta no puede superar " + MAX_PREGUNTA + " caracteres.");
            return "redirect:/asistente";
        }
        List<Mensaje> historial = historial(sesion);
        try {
            String respuesta = nexus.consultar(texto);
            agregar(historial, new Mensaje("Tú", texto));
            agregar(historial, new Mensaje("Nexus", respuesta.isBlank() ? "Sin respuesta." : respuesta));
        } catch (HttpStatusCodeException e) {
            redireccion.addFlashAttribute("error", mensajeNexus(e.getStatusCode().value()));
        } catch (RestClientException e) {
            redireccion.addFlashAttribute("error", "Nexus no está disponible. Intenta más tarde.");
        }
        sesion.setAttribute(CLAVE_SESION, historial);
        return "redirect:/asistente";
    }

    @PostMapping("/limpiar")
    public String limpiar(HttpSession sesion) {
        sesion.removeAttribute(CLAVE_SESION);
        return "redirect:/asistente";
    }

    private static List<Mensaje> historial(HttpSession sesion) {
        Object actual = sesion.getAttribute(CLAVE_SESION);
        if (actual instanceof List<?> lista) {
            @SuppressWarnings("unchecked")
            List<Mensaje> mensajes = (List<Mensaje>) lista;
            return mensajes;
        }
        return new ArrayList<>();
    }

    private static void agregar(List<Mensaje> historial, Mensaje mensaje) {
        historial.add(mensaje);
        while (historial.size() > MAX_MENSAJES) {
            historial.remove(0);
        }
    }

    private static String mensajeNexus(int estado) {
        return switch (estado) {
            case 429 -> "Se agotó tu presupuesto de consultas al asistente por ahora.";
            case 504 -> "El asistente tardó demasiado en responder. Intenta de nuevo.";
            case 401, 403 -> "Tu sesión o tu rol no permiten usar el asistente.";
            default -> "El asistente no pudo responder (error " + estado + ").";
        };
    }
}
