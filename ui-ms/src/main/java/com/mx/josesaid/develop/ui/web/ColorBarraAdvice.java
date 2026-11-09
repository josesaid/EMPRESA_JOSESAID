package com.mx.josesaid.develop.ui.web;

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Elige el color de la barra lateral según el rol del usuario. Si tiene varios roles, gana el de mayor prioridad.
 */
@ControllerAdvice
public class ColorBarraAdvice {

    static final String POR_DEFECTO = "azul";

    private static final List<String> PRIORIDAD = List.of(
            "ADMIN", "DIRECCION", "FINANZAS", "CONTADOR", "VENTAS", "COMPRAS",
            "ALMACEN", "OPERACIONES", "CALIDAD", "LEGAL", "JEFE_AREA");

    private static final Map<String, String> COLORES = Map.ofEntries(
            entry("ADMIN", "morado"),
            entry("DIRECCION", "rojo"),
            entry("VENTAS", "azul"),
            entry("COMPRAS", "naranja"),
            entry("ALMACEN", "verde"),
            entry("OPERACIONES", "cafe"),
            entry("FINANZAS", "rosa"),
            entry("CONTADOR", "rosa"),
            entry("CALIDAD", "verde"),
            entry("LEGAL", "rojo"),
            entry("JEFE_AREA", "azul"));

    @ModelAttribute("colorBarra")
    public String colorBarra(Authentication usuario) {
        if (usuario == null) {
            return POR_DEFECTO;
        }
        Set<String> roles = usuario.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
        return PRIORIDAD.stream()
                .filter(roles::contains)
                .findFirst()
                .map(COLORES::get)
                .orElse(POR_DEFECTO);
    }
}
