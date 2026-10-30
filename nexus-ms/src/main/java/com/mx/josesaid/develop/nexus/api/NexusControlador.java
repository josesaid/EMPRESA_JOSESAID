package com.mx.josesaid.develop.nexus.api;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.PreguntaAsistente;

@RestController
@RequestMapping("/nexus")
@Validated
public class NexusControlador {

    private final AsistenteIA asistente;

    public NexusControlador(AsistenteIA asistente) {
        this.asistente = asistente;
    }

    @PostMapping("/consultas")
    @PreAuthorize("hasAnyRole('DIRECCION','JEFE_AREA','VENTAS','ADMIN')")
    public ConsultaResponse consultar(@RequestBody ConsultaRequest solicitud,
                                      @AuthenticationPrincipal Jwt jwt) {
        var respuesta = asistente.consultar(new PreguntaAsistente(jwt.getSubject(), solicitud.pregunta()));
        return new ConsultaResponse(respuesta.texto());
    }
}
