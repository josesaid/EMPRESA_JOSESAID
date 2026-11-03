package com.mx.josesaid.develop.ui.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.mx.josesaid.develop.ui.seguridad.ServicioCuenta;
import com.mx.josesaid.develop.ui.seguridad.ServicioCuenta.ErrorCambioPassword;

@Controller
@RequestMapping("/cuenta")
public class CambioPasswordController {

    private final ServicioCuenta servicio;

    public CambioPasswordController(ServicioCuenta servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/cambiar-password")
    public String formulario() {
        return "cuenta/cambiar-password";
    }

    @PostMapping("/cambiar-password")
    public String cambiar(@RequestParam String correo,
                          @RequestParam String passwordActual,
                          @RequestParam String passwordNueva,
                          @RequestParam String confirmacion,
                          Model modelo) {
        modelo.addAttribute("correo", correo);
        if (!passwordNueva.equals(confirmacion)) {
            modelo.addAttribute("error", "La confirmación no coincide con la contraseña nueva.");
            return "cuenta/cambiar-password";
        }
        try {
            servicio.cambiarPasswordInicial(correo.trim(), passwordActual, passwordNueva);
            return "redirect:/login?cambiada";
        } catch (ErrorCambioPassword e) {
            modelo.addAttribute("error", e.getMessage());
            return "cuenta/cambiar-password";
        }
    }
}
