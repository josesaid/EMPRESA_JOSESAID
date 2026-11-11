package com.mx.josesaid.develop.ui.web;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import com.mx.josesaid.develop.ui.contabilidad.ContabilidadCliente;

/** Pantalla del catálogo de cuentas contables. */
@Controller
@RequestMapping("/cuentas")
public class CuentasController {

    private final ContabilidadCliente contabilidad;

    /** Recibe el servicio de contabilidad. */
    public CuentasController(ContabilidadCliente contabilidad) {
        this.contabilidad = contabilidad;
    }

    /** Muestra el listado de cuentas. */
    @GetMapping
    public String listado(Authentication usuario, Model modelo) {
        modelo.addAttribute("correo", usuario.getName());
        modelo.addAttribute("titulo", "Cuentas contables");
        try {
            modelo.addAttribute("cuentas", contabilidad.listarCuentas());
        } catch (HttpClientErrorException.Forbidden e) {
            modelo.addAttribute("cuentas", List.of());
            modelo.addAttribute("error", "Tu rol no tiene acceso al catálogo de cuentas contables.");
        } catch (RestClientException e) {
            modelo.addAttribute("cuentas", List.of());
            modelo.addAttribute("error", "No se pudo consultar el catálogo de cuentas. Intenta más tarde.");
        }
        return "cuentas/listado";
    }
}
