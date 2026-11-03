package com.mx.josesaid.develop.nexus.api;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.mx.josesaid.develop.nexus.application.AccionesPendientes;
import com.mx.josesaid.develop.nexus.application.AccionNoEncontradaException;
import com.mx.josesaid.develop.nexus.application.AccionNoPendienteException;

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
import com.mx.josesaid.develop.nexus.application.PresupuestoTokens;
import com.mx.josesaid.develop.nexus.application.RegistroConsulta;
import com.mx.josesaid.develop.nexus.comun.filtro.FiltroDatosSensibles;

@RestController
@RequestMapping("/nexus")
@Validated
public class NexusControlador {

    private final AsistenteIA asistente;
    private final AuditoriaConsultas auditoria;
    private final AccionesPendientes acciones;
    private final PresupuestoTokens presupuesto;

    public NexusControlador(AsistenteIA asistente, AuditoriaConsultas auditoria, AccionesPendientes acciones,
                            PresupuestoTokens presupuesto) {
        this.asistente = asistente;
        this.auditoria = auditoria;
        this.acciones = acciones;
        this.presupuesto = presupuesto;
    }

    /** El usuario confirma una acción que Nexus preparó. Solo quien la preparó puede confirmarla. */
    @PostMapping("/acciones/{id}/confirmacion")
    @PreAuthorize("hasAnyRole('VENTAS','CONTADOR')")
    public ResponseEntity<String> confirmarAccion(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        String respuesta = acciones.confirmar(id, jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body(respuesta);
    }

    /** Muestra al dueño la acción pendiente antes de confirmarla. */
    @GetMapping("/acciones/{id}")
    @PreAuthorize("hasAnyRole('VENTAS','CONTADOR')")
    public AccionResumen consultarAccion(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        var accion = acciones.consultar(id, jwt.getSubject());
        return new AccionResumen(accion.id(), accion.tipo(), accion.resumen(), accion.estatus(), accion.expiraUtc());
    }

    /** El usuario descarta una acción pendiente sin ejecutarla. */
    @PostMapping("/acciones/{id}/cancelacion")
    @PreAuthorize("hasAnyRole('VENTAS','CONTADOR')")
    public ResponseEntity<Void> cancelarAccion(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        acciones.cancelar(id, jwt.getSubject());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(AccionNoEncontradaException.class)
    ResponseEntity<String> noEncontrada(AccionNoEncontradaException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
    }

    @ExceptionHandler(AccionNoPendienteException.class)
    ResponseEntity<String> noPendiente(AccionNoPendienteException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
    }

    @PostMapping("/consultas")
    @PreAuthorize("isAuthenticated()")
    public ConsultaResponse consultar(@RequestBody @Valid ConsultaRequest solicitud,
                                      @AuthenticationPrincipal Jwt jwt) {
        String usuario = jwt.getSubject();
        String usuarioAuditoria = jwt.getClaimAsString("correo");
        if (usuarioAuditoria == null || usuarioAuditoria.isBlank()) {
            usuarioAuditoria = usuario;
        }
        presupuesto.exigirDisponible(usuarioAuditoria);
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
