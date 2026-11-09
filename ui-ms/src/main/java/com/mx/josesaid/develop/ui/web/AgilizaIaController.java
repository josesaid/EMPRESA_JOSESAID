package com.mx.josesaid.develop.ui.web;

import com.mx.josesaid.develop.ui.ia.SugerenciasIa;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/** "Agiliza con IA": muestra las opciones del rol del usuario y los pasos de la que elija. */
@Controller
public class AgilizaIaController {

    @GetMapping("/agiliza-ia")
    public String agilizaIa(@RequestParam(required = false) String opcion, Authentication usuario, Model modelo) {
        Set<String> roles = usuario.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
        modelo.addAttribute("titulo", "Agiliza con IA");
        modelo.addAttribute("seccion", "agiliza");
        modelo.addAttribute("opciones", SugerenciasIa.opciones(roles));
        modelo.addAttribute("elegida", SugerenciasIa.opcion(roles, opcion).orElse(null));
        return "agiliza/ia";
    }
}
