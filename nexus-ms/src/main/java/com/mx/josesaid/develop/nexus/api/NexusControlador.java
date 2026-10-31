package com.mx.josesaid.develop.nexus.api;

import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

import com.mx.josesaid.develop.nexus.application.AsistenteIA;
import com.mx.josesaid.develop.nexus.application.AuditoriaConsultas;
import com.mx.josesaid.develop.nexus.application.PreguntaAsistente;
import com.mx.josesaid.develop.nexus.application.RegistroConsulta;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

@RestController
@RequestMapping("/nexus")
@Validated
public class NexusControlador {

    private final AsistenteIA asistente;
    private final AuditoriaConsultas auditoria;

    public NexusControlador(AsistenteIA asistente, AuditoriaConsultas auditoria) {
        this.asistente = asistente;
        this.auditoria = auditoria;
    }

    @PostMapping("/consultas")
    @PreAuthorize("hasAnyRole('DIRECCION','JEFE_AREA','VENTAS','ADMIN')")
    public ConsultaResponse consultar(@RequestBody @Valid ConsultaRequest solicitud,
                                      @AuthenticationPrincipal Jwt jwt) {
        String usuario = jwt.getSubject();
        String usuarioAuditoria = jwt.getClaimAsString("correo");
        if (usuarioAuditoria == null || usuarioAuditoria.isBlank()) {
            usuarioAuditoria = usuario;
        }
        var respuesta = asistente.consultar(new PreguntaAsistente(usuario, solicitud.pregunta()));
        String texto = respuesta.texto() == null ? "" : respuesta.texto();
        String resumen = FiltroDatosSensibles.redactar(texto.length() > 500 ? texto.substring(0, 500) : texto).texto();
        String herramientas = String.join(",", respuesta.herramientas());
        if (herramientas.length() > 500) {
            herramientas = herramientas.substring(0, 500);
        }
        auditoria.registrar(new RegistroConsulta(
                usuarioAuditoria,
                FiltroDatosSensibles.redactar(solicitud.pregunta()).texto(),
                resumen,
                herramientas.isEmpty() ? null : herramientas,
                respuesta.tokensEntrada(),
                respuesta.tokensSalida(),
                Instant.now()));
        return new ConsultaResponse(FiltroDatosSensibles.redactar(texto).texto());
    }
}
