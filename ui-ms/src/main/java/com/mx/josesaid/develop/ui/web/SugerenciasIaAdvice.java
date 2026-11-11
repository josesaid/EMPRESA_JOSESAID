package com.mx.josesaid.develop.ui.web;

import com.mx.josesaid.develop.ui.ia.SugerenciasIa;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Pone el log de sugerencias de IA en todas las pantallas. Sin sesión no hay log. */
@ControllerAdvice
public class SugerenciasIaAdvice {

    static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    /** Agrega al modelo las sugerencias del catálogo para el menú de ayuda. */
    @ModelAttribute("sugerenciasIa")
    public List<SugerenciasIa.Sugerencia> sugerenciasIa(Authentication usuario) {
        if (usuario == null) {
            return null;
        }
        Set<String> roles = usuario.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toSet());
        return SugerenciasIa.para(roles, LocalTime.now(ZONA));
    }
}
