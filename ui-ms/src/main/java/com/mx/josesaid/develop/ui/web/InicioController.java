package com.mx.josesaid.develop.ui.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InicioController {

    @GetMapping("/")
    public String inicio(Model modelo) {
        modelo.addAttribute("titulo", "Inicio");
        return "inicio";
    }
}
