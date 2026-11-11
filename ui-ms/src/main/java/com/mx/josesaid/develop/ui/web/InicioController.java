package com.mx.josesaid.develop.ui.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/** Pantalla de inicio. */
@Controller
public class InicioController {

    /** Muestra la pantalla de inicio con el usuario y su sección. */
    @GetMapping("/")
    public String inicio(Authentication usuario, Model modelo) {
        modelo.addAttribute("titulo", "Inicio");
        modelo.addAttribute("correo", usuario.getName());
        modelo.addAttribute("roles", usuario.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", "")).toList());
        return "inicio";
    }
}
