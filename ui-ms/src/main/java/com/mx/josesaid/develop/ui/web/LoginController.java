package com.mx.josesaid.develop.ui.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** Pantalla pública de inicio de sesión. */
@Controller
public class LoginController {

    /** Muestra el formulario de inicio de sesión. */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
