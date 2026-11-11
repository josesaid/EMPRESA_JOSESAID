package com.mx.josesaid.develop.ui.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** Sesión que no puede renovarse: se cierra localmente y se pide volver a entrar. */
@ControllerAdvice
public class ManejadorSesionExpirada {

    /** Responde a una petición con sesión expirada: redirige al login con un aviso. */
    @ExceptionHandler(SesionExpiradaException.class)
    public String sesionExpirada(HttpServletRequest peticion) {
        HttpSession sesion = peticion.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        return "redirect:/login?expirada";
    }
}
