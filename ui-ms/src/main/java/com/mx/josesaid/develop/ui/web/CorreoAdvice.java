package com.mx.josesaid.develop.ui.web;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Pone el correo del usuario logueado en el encabezado de todas las pantallas. */
@ControllerAdvice
public class CorreoAdvice {

    @ModelAttribute("correo")
    public String correo(Authentication usuario) {
        if (usuario == null || !usuario.isAuthenticated() || usuario instanceof AnonymousAuthenticationToken) {
            return null;
        }
        return usuario.getName();
    }
}
